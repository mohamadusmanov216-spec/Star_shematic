package sbuild.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import sbuild.state.AppContext;

/**
 * Handles all command execution logic for /sbuild commands.
 *
 * <p>This class is intentionally thin and delegates behavior to module services as features
 * are implemented.</p>
 */
public final class SBuildCommandHandler {
    private final AppContext context;

    public SBuildCommandHandler(AppContext context) {
        this.context = context;
    }

    public int handleRoot(CommandContext<ServerCommandSource> commandContext) {
        context.commandService(); // Ensures command module dependency is part of execution path.
        commandContext.getSource().sendFeedback(() -> Text.literal("SBuild initialized. Use /sbuild help."), false);
        return 1;
    }

    public int handleStatus(CommandContext<ServerCommandSource> commandContext) {
        boolean hasActiveBuild = context.buildStateService().hasActiveBuild();
        String message = hasActiveBuild ? "SBuild status: build in progress." : "SBuild status: idle.";
        commandContext.getSource().sendFeedback(() -> Text.literal(message), false);
        return 1;
    }

    public int handleHelp(CommandContext<ServerCommandSource> commandContext) {
        commandContext.getSource().sendFeedback(() -> Text.literal("SBuild commands: /sbuild, /sbuild status, /sbuild help"), false);
        return 1;
    }
}
