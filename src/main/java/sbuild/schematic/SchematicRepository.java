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
 * In-memory repository of discovered/loaded schematics.
 */
public final class SchematicRepository {
    private final Path rootDirectory;
    private final SchematicLoader loader;
    private final ConcurrentMap<Path, LoadedSchematic> cacheByPath;
    private final ConcurrentMap<String, Path> pathByName;

    public SchematicRepository(SchematicLoader loader) {
        this(defaultRootDirectory(), loader);
    }

    public SchematicRepository(Path rootDirectory, SchematicLoader loader) {
        this.rootDirectory = rootDirectory;
        this.loader = loader;
        this.cacheByPath = new ConcurrentHashMap<>();
        this.pathByName = new ConcurrentHashMap<>();
    }

    public Path rootDirectory() {
        return rootDirectory;
    }

    public List<Path> scan() {
        ensureRootExists();
        try (var stream = Files.list(rootDirectory)) {
            List<Path> paths = stream
                .filter(Files::isRegularFile)
                .filter(this::isSupportedSchematic)
                .map(path -> path.toAbsolutePath().normalize())
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                .toList();
            indexNames(paths);
            return paths;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan schematics directory: " + rootDirectory, e);
        }
    }

    public LoadedSchematic load(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        return cacheByPath.computeIfAbsent(normalized, key -> {
            try {
                LoadedSchematic loaded = loader.load(key);
                pathByName.put(loaded.name().toLowerCase(Locale.ROOT), key);
                return loaded;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load schematic: " + key, e);
            }
        });
    }

    public Optional<LoadedSchematic> loadByName(String name) {
        Path path = pathByName.get(name.toLowerCase(Locale.ROOT));
        if (path != null) {
            return Optional.of(load(path));
        }
        for (Path candidate : scan()) {
            String base = baseName(candidate).toLowerCase(Locale.ROOT);
            if (base.equals(name.toLowerCase(Locale.ROOT))) {
                return Optional.of(load(candidate));
            }
        }
        return Optional.empty();
    }

    public List<LoadedSchematic> loadAll() {
        List<LoadedSchematic> loaded = new ArrayList<>();
        for (Path path : scan()) {
            loaded.add(load(path));
        }
        return List.copyOf(loaded);
    }

    public Optional<LoadedSchematic> findById(String id) {
        return cacheByPath.values().stream().filter(s -> s.id().equals(id)).findFirst();
    }

    public void clearCache() {
        cacheByPath.clear();
    }

    private void ensureRootExists() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create schematic directory: " + rootDirectory, e);
        }
    }

    private void indexNames(List<Path> paths) {
        for (Path path : paths) {
            pathByName.put(baseName(path).toLowerCase(Locale.ROOT), path);
        }
    }

    private String baseName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private boolean isSupportedSchematic(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".litematic");
    }

    private static Path defaultRootDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve("sbuild").resolve("schematics");
    }
}
