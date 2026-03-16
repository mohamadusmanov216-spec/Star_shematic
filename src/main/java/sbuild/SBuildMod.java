package sbuild;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sbuild.ai.AiService;
import sbuild.block.BlockPlacementService;
import sbuild.bot.BuildBotService;
import sbuild.command.SBuildCommand;
import sbuild.command.SBuildCommandHandler;
import sbuild.config.ConfigService;
import sbuild.command.CommandService;
import sbuild.gui.GuiService;
import sbuild.materials.MaterialAnalysisService;
import sbuild.planner.BuildPlannerService;
import sbuild.redstone.RedstoneService;
import sbuild.render.RenderService;
import sbuild.schematic.SchematicService;
import sbuild.state.AppContext;
import sbuild.state.BuildStateService;
import sbuild.storage.StorageService;
import sbuild.util.ModuleBootstrap;
import sbuild.util.UtilityService;
import sbuild.world.WorldService;

/**
 * Main Fabric entrypoint for SBuild.
 *
 * <p>This class wires module services together and registers top-level integration points
 * (commands, future events, etc.) while keeping feature logic in dedicated modules.</p>
 */
public final class SBuildMod implements ModInitializer {
    public static final String MOD_ID = "sbuild";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AppContext context = createContext();
        registerCommands(context);
        LOGGER.info("SBuild base architecture initialized.");
    }

    private AppContext createContext() {
        ConfigService configService = new ConfigService();
        BuildStateService stateService = new BuildStateService();
        CommandService commandService = new CommandService();
        SchematicService schematicService = new SchematicService(configService);
        GuiService guiService = new GuiService();
        RenderService renderService = new RenderService(schematicService, stateService);
        WorldService worldService = new WorldService();
        MaterialAnalysisService materialAnalysisService = new MaterialAnalysisService();
        StorageService storageService = new StorageService();
        BuildPlannerService plannerService = new BuildPlannerService();
        AiService aiService = new AiService();
        RedstoneService redstoneService = new RedstoneService();
        BlockPlacementService blockPlacementService = new BlockPlacementService();
        BuildBotService buildBotService = new BuildBotService();
        UtilityService utilityService = new UtilityService();

        AppContext context = new AppContext(
            configService,
            commandService,
            stateService,
            schematicService,
            guiService,
            renderService,
            worldService,
            materialAnalysisService,
            storageService,
            plannerService,
            aiService,
            redstoneService,
            blockPlacementService,
            buildBotService,
            utilityService
        );

        ModuleBootstrap bootstrap = new ModuleBootstrap(context);
        bootstrap.initializeCoreModules();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            bootstrap.initializeClientModules();
        }
        return context;
    }

    private void registerCommands(AppContext context) {
        SBuildCommandHandler handler = new SBuildCommandHandler(context.buildStateService());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            SBuildCommand.register(dispatcher, handler)
        );
    }
}
