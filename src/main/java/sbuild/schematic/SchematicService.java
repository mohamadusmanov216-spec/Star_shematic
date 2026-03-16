package sbuild.schematic;

import sbuild.config.ConfigService;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Фасад модуля схематик: сканирование, загрузка и API размещения.
 */
public final class SchematicService {
    @SuppressWarnings("unused")
    private final ConfigService configService;
    private final SchematicRepository repository;

    public SchematicService(ConfigService configService) {
        this(configService, new SchematicRepository(new SchematicLoader()));
    }

    public SchematicService(ConfigService configService, SchematicRepository repository) {
        this.configService = configService;
        this.repository = repository;
    }

    public void initialize() {
        repository.initialize();
    }

    public Path rootDirectory() {
        return repository.rootDirectory();
    }

    public List<Path> scanSchematics() {
        return repository.scan();
    }

    public LoadedSchematic load(Path path) {
        return repository.load(path);
    }

    public List<LoadedSchematic> loadAll() {
        return repository.loadAll();
    }

    public Optional<LoadedSchematic> findCachedById(String id) {
        return repository.findById(id);
    }

    public PlacementController createPlacementController(LoadedSchematic schematic, SchematicTransform transform) {
        return new PlacementController(schematic, transform);
    }
}
