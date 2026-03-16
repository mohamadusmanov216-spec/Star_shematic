package sbuild.util;

import sbuild.state.AppContext;

public final class ModuleBootstrap {
    private final AppContext context;

    public ModuleBootstrap(AppContext context) {
        this.context = context;
    }

    public void initializeCoreModules() {
        context.buildBotService().initialize();
    }

    public void initializeClientModules() {
        context.guiService().initialize();
        context.renderService().initialize();
    }
}
