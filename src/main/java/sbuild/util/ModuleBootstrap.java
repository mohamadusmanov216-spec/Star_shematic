package sbuild.util;

import sbuild.state.AppContext;

/**
 * Centralized module lifecycle bootstrap.
 *
 * <p>This creates one place where startup ordering can evolve as module dependencies grow.</p>
 */
public final class ModuleBootstrap {
    private final AppContext context;

    public ModuleBootstrap(AppContext context) {
        this.context = context;
    }

    /**
     * Initializes all registered modules in predictable order.
     */
    public void initializeModules() {
        context.configService().initialize();
        context.commandService().initialize();
        context.buildStateService().initialize();
        context.schematicService().initialize();
        context.guiService().initialize();
        context.renderService().initialize();
        context.worldService().initialize();
        context.materialAnalysisService().initialize();
        context.storageService().initialize();
        context.plannerService().initialize();
        context.aiService().initialize();
        context.redstoneService().initialize();
        context.blockPlacementService().initialize();
        context.buildBotService().initialize();
        context.utilityService().initialize();
    }
}
