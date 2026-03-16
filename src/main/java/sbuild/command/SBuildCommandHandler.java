package sbuild.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import sbuild.state.BuildStateService;
import sbuild.storage.StoragePoint;
import sbuild.storage.StorageService;

/**
 * Handles all command execution logic for /sbuild commands.
 *
 * <p>This class is intentionally thin and delegates behavior to module services as features
 * are implemented.</p>
 */
public final class SBuildCommandHandler {
    private final BuildStateService buildStateService;
    private final StorageService storageService;

    public SBuildCommandHandler(BuildStateService buildStateService, StorageService storageService) {
        this.buildStateService = buildStateService;
        this.storageService = storageService;
    }

    public int handleRoot(CommandContext<ServerCommandSource> commandContext) {
        commandContext.getSource().sendFeedback(() -> Text.literal("SBuild initialized. Use /sbuild help."), false);
        return 1;
    }

    public int handleStatus(CommandContext<ServerCommandSource> commandContext) {
        boolean hasActiveBuild = buildStateService.hasActiveBuild();
        String message = hasActiveBuild ? "SBuild status: build in progress." : "SBuild status: idle.";
        commandContext.getSource().sendFeedback(() -> Text.literal(message), false);
        return 1;
    }

    public int handleHelp(CommandContext<ServerCommandSource> commandContext) {
        commandContext.getSource().sendFeedback(() -> Text.literal("SBuild commands: /sbuild, /sbuild status, /sbuild help, /sbuild chest set <name>, /sbuild chest list"), false);
        return 1;
    }

    public int handleChestSet(CommandContext<ServerCommandSource> commandContext, String name) {
        ServerCommandSource source = commandContext.getSource();
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = player.getServerWorld();
            StoragePoint point = storageService.registerLookedAtChest(world, player, name);
            source.sendFeedback(() -> Text.literal("Storage chest registered: " + point.name() + " @ " + formatPos(point)), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Не удалось зарегистрировать сундук: " + e.getMessage()));
            return 0;
        }
    }

    public int handleChestList(CommandContext<ServerCommandSource> commandContext) {
        ServerCommandSource source = commandContext.getSource();
        var points = storageService.listStoragePoints();
        if (points.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Сундуки хранения не зарегистрированы."), false);
            return 1;
        }

        source.sendFeedback(() -> Text.literal("Зарегистрированные storage chest: " + points.size()), false);
        for (StoragePoint point : points) {
            source.sendFeedback(() -> Text.literal("- " + point.name() + " @ " + formatPos(point) + " priority=" + point.priority()), false);
        }
        return 1;
    }

    private String formatPos(StoragePoint point) {
        return point.blockPos().getX() + "," + point.blockPos().getY() + "," + point.blockPos().getZ();
    }
}
