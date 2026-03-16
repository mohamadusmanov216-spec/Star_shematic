package sbuild.schematic;

import sbuild.config.ConfigService;

/**
 * Responsible for schematic discovery, parsing, and in-memory model access.
 */
public final class SchematicService {
    @SuppressWarnings("unused")
    private final ConfigService configService;

    public SchematicService(ConfigService configService) {
        this.configService = configService;
    }

    public void initialize() {
        // Stub: schematic repository setup will be implemented later.
    }
}
