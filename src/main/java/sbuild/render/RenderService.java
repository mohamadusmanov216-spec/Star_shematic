package sbuild.render;

import sbuild.schematic.SchematicService;
import sbuild.state.BuildStateService;

/**
 * Handles client-side visualization, including ghost blocks and overlays.
 */
public final class RenderService {
    @SuppressWarnings("unused")
    private final SchematicService schematicService;
    @SuppressWarnings("unused")
    private final BuildStateService buildStateService;

    public RenderService(SchematicService schematicService, BuildStateService buildStateService) {
        this.schematicService = schematicService;
        this.buildStateService = buildStateService;
    }

    public void initialize() {
        // Stub: renderer hooks will be implemented later.
    }
}
