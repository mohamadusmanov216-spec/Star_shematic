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
     * Initializes server-safe core modules in predictable order.
     */
    public void initializeCoreModules() {
        context.configService().initialize();
        context.commandService().initialize();
        context.buildStateService().initialize();
        context.schematicService().initialize();
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

    /**
     * Initializes client-only modules.
     */
    public void initializeClientModules() {
        context.guiService().initialize();
        context.renderService().initialize();
    }
}
