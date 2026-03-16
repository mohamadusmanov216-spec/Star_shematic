package sbuild.bot;

import net.minecraft.block.Block;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-level orchestrator of bot actions with per-player task queues.
 */
public final class BotController {
    private static final int MOVE_TIMEOUT_TICKS = 120;
    private static final int PLACE_TIMEOUT_TICKS = 30;
    private static final int BREAK_TIMEOUT_TICKS = 60;
    private static final int SCAFFOLD_TIMEOUT_TICKS = 30;
    private static final int LOOK_TIMEOUT_TICKS = 10;
    private static final int MAX_RETRIES = 2;

    private final PathExecutor pathExecutor;
    private final LookController lookController;
    private final InventoryController inventoryController;
    private final PlaceController placeController;
    private final BreakController breakController;
    private final JumpScaffoldController jumpScaffoldController;
    private final TowerBuilder towerBuilder;
    private final SafetyController safetyController;
    private final RecoveryController recoveryController;

    private final Map<UUID, BotRuntime> runtimes = new ConcurrentHashMap<>();

    public BotController(
        PathExecutor pathExecutor,
        LookController lookController,
        InventoryController inventoryController,
        PlaceController placeController,
        BreakController breakController,
        JumpScaffoldController jumpScaffoldController,
        TowerBuilder towerBuilder,
        SafetyController safetyController,
        RecoveryController recoveryController
    ) {
        this.pathExecutor = pathExecutor;
        this.lookController = lookController;
        this.inventoryController = inventoryController;
        this.placeController = placeController;
        this.breakController = breakController;
        this.jumpScaffoldController = jumpScaffoldController;
        this.towerBuilder = towerBuilder;
        this.safetyController = safetyController;
        this.recoveryController = recoveryController;
    }

    public void enqueueMoveTo(ServerPlayerEntity player, BlockPos target) {
        BotRuntime runtime = runtime(player);
        runtime.actions.add(new MoveAction(target.toImmutable()));
    }

    public void enqueueRotateTo(ServerPlayerEntity player, BlockPos target) {
        BotRuntime runtime = runtime(player);
        runtime.actions.add(new LookAction(target.toImmutable()));
    }

    public void enqueuePlaceBlock(ServerPlayerEntity player, BlockPos target, Block block) {
        BotRuntime runtime = runtime(player);
        runtime.actions.add(new PlaceAction(target.toImmutable(), block));
    }

    public void enqueueBreakBlock(ServerPlayerEntity player, BlockPos target) {
        BotRuntime runtime = runtime(player);
        runtime.actions.add(new BreakAction(target.toImmutable()));
    }

    public void enqueueScaffold(ServerPlayerEntity player, Block scaffoldBlock) {
        BotRuntime runtime = runtime(player);
        runtime.actions.add(new ScaffoldAction(scaffoldBlock));
    }

    public void enqueueTowerBuild(ServerPlayerEntity player, int targetY, Block scaffoldBlock) {
        BotRuntime runtime = runtime(player);
        runtime.actions.add(new TowerAction(targetY, scaffoldBlock));
    }

    public BotState state(ServerPlayerEntity player) {
        return runtime(player).state;
    }

    public void clearTasks(ServerPlayerEntity player) {
        BotRuntime runtime = runtime(player);
        runtime.actions.clear();
        runtime.path.clear();
        ServerWorld world = player.getWorld();
        runtime.guard.reset(world.getTime());
        runtime.state = BotState.idle(world.getTime());
    }

    public void tickAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            BotRuntime runtime = runtimes.get(player.getUuid());
            if (runtime == null) {
                continue;
            }
            tickRuntime(player.getWorld(), player, runtime);
        }
    }

    private void tickRuntime(ServerWorld world, ServerPlayerEntity player, BotRuntime runtime) {
        if (safetyController.shouldRecover(player)) {
            recoveryController.recover(world, player);
            runtime.guard.reset(world.getTime());
            runtime.state = runtime.state.with(BotState.Status.RECOVERING, player.getBlockPos(), "recovering", world.getTime());
            return;
        }

        BotAction action = runtime.actions.peek();
        if (action == null) {
            runtime.guard.reset(world.getTime());
            runtime.state = BotState.idle(world.getTime());
            return;
        }

        runtime.guard.begin(action, world.getTime());

        try {
            boolean done = action.execute(world, player, runtime, world.getTime());
            if (done) {
                runtime.actions.poll();
                runtime.path.clear();
                runtime.guard.reset(world.getTime());
                return;
            }

            if (runtime.guard.isTimedOut(world.getTime(), timeoutTicks(action))) {
                handleActionFailure(player, runtime, "action-timeout");
            }
        } catch (RuntimeException exception) {
            handleActionFailure(player, runtime, exception.getMessage());
        }
    }

    private int timeoutTicks(BotAction action) {
        if (action instanceof MoveAction) {
            return MOVE_TIMEOUT_TICKS;
        }
        if (action instanceof PlaceAction) {
            return PLACE_TIMEOUT_TICKS;
        }
        if (action instanceof BreakAction) {
            return BREAK_TIMEOUT_TICKS;
        }
        if (action instanceof ScaffoldAction || action instanceof TowerAction) {
            return SCAFFOLD_TIMEOUT_TICKS;
        }
        return LOOK_TIMEOUT_TICKS;
    }

    private void handleActionFailure(ServerPlayerEntity player, BotRuntime runtime, String reason) {
        BotActionGuard.FailureDecision decision = runtime.guard.onFailure(player.getWorld().getTime());
        if (decision == BotActionGuard.FailureDecision.RETRY) {
            runtime.path.clear();
            runtime.state = runtime.state.with(
                BotState.Status.RECOVERING,
                player.getBlockPos(),
                "retry-" + runtime.guard.retryCount() + ":" + reason,
                player.getWorld().getTime()
            );
            return;
        }

        runtime.actions.poll();
        runtime.path.clear();
        runtime.state = runtime.state.with(BotState.Status.ERROR, player.getBlockPos(), reason, player.getWorld().getTime());
    }

    private BotRuntime runtime(ServerPlayerEntity player) {
        return runtimes.computeIfAbsent(player.getUuid(), ignored -> new BotRuntime(player.getWorld().getTime()));
    }

    private final class BotRuntime {
        private final Deque<BotAction> actions = new ArrayDeque<>();
        private final Deque<BlockPos> path = new ArrayDeque<>();
        private final BotActionGuard guard;
        private BotState state;

        private BotRuntime(long tick) {
            this.state = BotState.idle(tick);
            this.guard = new BotActionGuard(MAX_RETRIES, tick);
        }
    }

    private interface BotAction {
        boolean execute(ServerWorld world, ServerPlayerEntity player, BotRuntime runtime, long tick);
    }

    private final class MoveAction implements BotAction {
        private final BlockPos target;

        private MoveAction(BlockPos target) {
            this.target = target;
        }

        @Override
        public boolean execute(ServerWorld world, ServerPlayerEntity player, BotRuntime runtime, long tick) {
            if (!safetyController.isSafeTarget(world, player, target)) {
                throw new IllegalStateException("Unsafe move target");
            }
            if (runtime.path.isEmpty()) {
                runtime.path.addAll(pathExecutor.createPath(world, player.getBlockPos(), target));
            }
            boolean done = pathExecutor.executeStep(player, runtime.path);
            runtime.state = runtime.state.with(BotState.Status.MOVING, target, "moving", tick);
            return done;
        }
    }

    private final class LookAction implements BotAction {
        private final BlockPos target;

        private LookAction(BlockPos target) {
            this.target = target;
        }

        @Override
        public boolean execute(ServerWorld world, ServerPlayerEntity player, BotRuntime runtime, long tick) {
            lookController.lookAtBlock(player, target);
            runtime.state = runtime.state.with(BotState.Status.LOOKING, target, "looking", tick);
            return true;
        }
    }

    private final class PlaceAction implements BotAction {
        private final BlockPos target;
        private final Block block;

        private PlaceAction(BlockPos target, Block block) {
            this.target = target;
            this.block = block;
        }

        @Override
        public boolean execute(ServerWorld world, ServerPlayerEntity player, BotRuntime runtime, long tick) {
            if (!safetyController.isSafeTarget(world, player, target)) {
                throw new IllegalStateException("Unsafe placement target");
            }
            if (!inventoryController.selectHotbarBlock(player, block)) {
                throw new IllegalStateException("Required block is missing in hotbar");
            }
            boolean placed = placeController.placeBlock(world, player, target, block);
            runtime.state = runtime.state.with(BotState.Status.PLACING, target, placed ? "placed" : "placement-failed", tick);
            return placed;
        }
    }

    private final class BreakAction implements BotAction {
        private final BlockPos target;

        private BreakAction(BlockPos target) {
            this.target = target;
        }

        @Override
        public boolean execute(ServerWorld world, ServerPlayerEntity player, BotRuntime runtime, long tick) {
            boolean done = breakController.breakBlock(world, player, target);
            runtime.state = runtime.state.with(BotState.Status.BREAKING, target, done ? "broken" : "break-failed", tick);
            return done;
        }
    }

    private final class ScaffoldAction implements BotAction {
        private final Block scaffoldBlock;

        private ScaffoldAction(Block scaffoldBlock) {
            this.scaffoldBlock = scaffoldBlock;
        }

        @Override
        public boolean execute(ServerWorld world, ServerPlayerEntity player, BotRuntime runtime, long tick) {
            boolean done = jumpScaffoldController.placeScaffoldBelow(world, player, scaffoldBlock);
            runtime.state = runtime.state.with(BotState.Status.SCAFFOLDING, player.getBlockPos(), done ? "scaffold-ready" : "scaffold-failed", tick);
            return done;
        }
    }

    private final class TowerAction implements BotAction {
        private final int targetY;
        private final Block scaffoldBlock;

        private TowerAction(int targetY, Block scaffoldBlock) {
            this.targetY = targetY;
            this.scaffoldBlock = scaffoldBlock;
        }

        @Override
        public boolean execute(ServerWorld world, ServerPlayerEntity player, BotRuntime runtime, long tick) {
            boolean done = towerBuilder.buildStepUp(world, player, targetY, scaffoldBlock);
            runtime.state = runtime.state.with(BotState.Status.SCAFFOLDING, player.getBlockPos(), done ? "tower-complete" : "tower-building", tick);
            return done;
        }
    }
}
