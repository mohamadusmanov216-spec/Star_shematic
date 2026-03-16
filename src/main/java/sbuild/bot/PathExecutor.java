package sbuild.bot;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Builds and executes lightweight block-to-block paths.
 */
public final class PathExecutor {
    private final MovementController movementController;
    private final LookController lookController;

    public PathExecutor(MovementController movementController, LookController lookController) {
        this.movementController = movementController;
        this.lookController = lookController;
    }

    public Deque<BlockPos> createPath(BlockPos from, BlockPos to) {
        List<BlockPos> points = new ArrayList<>();
        int x = from.getX();
        int y = from.getY();
        int z = from.getZ();

        while (x != to.getX()) {
            x += Integer.compare(to.getX(), x);
            points.add(new BlockPos(x, y, z));
        }
        while (z != to.getZ()) {
            z += Integer.compare(to.getZ(), z);
            points.add(new BlockPos(x, y, z));
        }
        while (y != to.getY()) {
            y += Integer.compare(to.getY(), y);
            points.add(new BlockPos(x, y, z));
        }
        return new ArrayDeque<>(points);
    }

    public boolean executeStep(ServerPlayerEntity player, Deque<BlockPos> path) {
        BlockPos next = path.peekFirst();
        if (next == null) {
            return true;
        }

        lookController.lookAt(player, Vec3d.ofCenter(next));
        boolean reached = movementController.moveTowards(player, next, 0.22D);
        if (reached) {
            path.pollFirst();
        }
        return path.isEmpty();
    }
}
