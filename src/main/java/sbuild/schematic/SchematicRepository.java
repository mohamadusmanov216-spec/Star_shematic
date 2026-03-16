package sbuild.schematic;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Репозиторий схематик: сканирование каталога, загрузка и кэш in-memory.
 */
public final class SchematicRepository {
    private final Path rootDirectory;
    private final SchematicLoader loader;
    private final ConcurrentMap<String, LoadedSchematic> cache;

    public SchematicRepository(SchematicLoader loader) {
        this(defaultRootDirectory(), loader);
    }

    public SchematicRepository(Path rootDirectory, SchematicLoader loader) {
        this.rootDirectory = rootDirectory;
        this.loader = loader;
        this.cache = new ConcurrentHashMap<>();
    }

    public void initialize() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create schematic directory: " + rootDirectory, e);
        }
    }

    public Path rootDirectory() {
        return rootDirectory;
    }

    public List<Path> scan() {
        if (!Files.isDirectory(rootDirectory)) {
            return List.of();
        }

        try (var stream = Files.list(rootDirectory)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(this::isSupportedSchematic)
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan schematics directory: " + rootDirectory, e);
        }
    }

    public List<LoadedSchematic> loadAll() {
        List<LoadedSchematic> loaded = new ArrayList<>();
        for (Path path : scan()) {
            loaded.add(load(path));
        }
        return List.copyOf(loaded);
    }

    public LoadedSchematic load(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        return cache.computeIfAbsent(normalized.toString(), key -> {
            try {
                return loader.load(normalized);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load schematic: " + normalized, e);
            }
        });
    }

    public Optional<LoadedSchematic> findById(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    public List<LoadedSchematic> cachedSchematics() {
        return List.copyOf(cache.values());
    }

    public void clearCache() {
        cache.clear();
    }

    private boolean isSupportedSchematic(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".litematic");
    }

    private static Path defaultRootDirectory() {
        return FabricLoader.getInstance()
            .getGameDir()
            .resolve("sbuild")
            .resolve("schematics");
    }
}
