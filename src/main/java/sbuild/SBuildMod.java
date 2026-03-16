package sbuild;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sbuild.ai.AiService;
import sbuild.block.BlockPlacementService;
import sbuild.bot.BuildBotService;
import sbuild.command.CommandService;
import sbuild.command.SBuildCommand;
import sbuild.command.SBuildCommandHandler;
import sbuild.config.ConfigService;
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

public final class SBuildMod implements ModInitializer {
    public static final String MOD_ID = "sbuild";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AppContext context = createContext();
        registerCommands(context);
        LOGGER.info("SBuild initialized.");
    }

    private AppContext createContext() {
        BuildStateService stateService = new BuildStateService();
        SchematicService schematicService = new SchematicService();
        WorldService worldService = new WorldService();
        MaterialAnalysisService materialAnalysisService = new MaterialAnalysisService();
        StorageService storageService = new StorageService();
        BuildPlannerService plannerService = new BuildPlannerService();

        AppContext context = new AppContext(
            new ConfigService(),
            new CommandService(),
            stateService,
            schematicService,
            new GuiService(),
            new RenderService(schematicService, stateService),
            worldService,
            materialAnalysisService,
            storageService,
            plannerService,
            new AiService(),
            new RedstoneService(),
            new BlockPlacementService(),
            new BuildBotService(),
            new UtilityService()
        );

        ModuleBootstrap bootstrap = new ModuleBootstrap(context);
        bootstrap.initializeCoreModules();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            bootstrap.initializeClientModules();
        }
        return context;
    }

    private void registerCommands(AppContext context) {
        SBuildCommandHandler handler = new SBuildCommandHandler(
            context.buildStateService(),
            context.schematicService(),
            context.worldService(),
            context.materialAnalysisService(),
            context.storageService(),
            context.plannerService()
        );
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            SBuildCommand.register(dispatcher, handler)
        );
    }
}
