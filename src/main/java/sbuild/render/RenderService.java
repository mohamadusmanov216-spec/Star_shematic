package sbuild.render;

import sbuild.schematic.SchematicService;
import sbuild.state.BuildStateService;

/**
 * Handles client-side visualization, including ghost blocks and overlays.
 */
public final class RenderService {
    private final SchematicService schematicService;
    private final BuildStateService buildStateService;

    public RenderService(SchematicService schematicService, BuildStateService buildStateService) {
        this.schematicService = schematicService;
        this.buildStateService = buildStateService;
    }

    public void initialize() {
        // Stub: renderer hooks will be implemented later.
    }

    public SchematicService schematicService() {
        return schematicService;
    }

    public BuildStateService buildStateService() {
        return buildStateService;
    }
}
