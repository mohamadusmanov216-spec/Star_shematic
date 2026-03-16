package sbuild.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import sbuild.state.BuildStateService;

/**
 * Handles all command execution logic for /sbuild commands.
 *
 * <p>This class is intentionally thin and delegates behavior to module services as features
 * are implemented.</p>
 */
public final class SBuildCommandHandler {
    private final BuildStateService buildStateService;

    public SBuildCommandHandler(BuildStateService buildStateService) {
        this.buildStateService = buildStateService;
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
        commandContext.getSource().sendFeedback(() -> Text.literal("SBuild commands: /sbuild, /sbuild status, /sbuild help"), false);
        return 1;
    }
}
