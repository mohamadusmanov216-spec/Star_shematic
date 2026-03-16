package sbuild.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Root command registration for SBuild.
 */
public final class SBuildCommand {
    private SBuildCommand() {
    }

    /**
     * Registers /sbuild and nested subcommands.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, SBuildCommandHandler handler) {
        dispatcher.register(
            CommandManager.literal("sbuild")
                .executes(handler::handleRoot)
                .then(CommandManager.literal("status").executes(handler::handleStatus))
                .then(CommandManager.literal("help").executes(handler::handleHelp))
                .then(CommandManager.literal("chest")
                    .then(CommandManager.literal("set")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> handler.handleChestSet(ctx, StringArgumentType.getString(ctx, "name")))))
                    .then(CommandManager.literal("list")
                        .executes(handler::handleChestList)))
        );
    }
}
