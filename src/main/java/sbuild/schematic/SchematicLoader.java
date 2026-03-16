package sbuild.schematic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Загружает схематики с диска в иммутабельную модель {@link LoadedSchematic}.
 *
 * <p>На текущем этапе поддерживается формат `.litematic` с базовой загрузкой
 * метаданных и подготовленной точкой расширения для парсинга блоков.</p>
 */
public final class SchematicLoader {
    private static final String SUPPORTED_EXTENSION = ".litematic";

    public LoadedSchematic load(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        validateFormat(normalized);

        String fileName = normalized.getFileName().toString();
        String baseName = fileName.substring(0, fileName.length() - SUPPORTED_EXTENSION.length());
        long fileSize = Files.size(normalized);
        FileTime lastModifiedTime = Files.getLastModifiedTime(normalized);
        Instant lastModified = lastModifiedTime.toInstant();

        Map<LoadedSchematic.BlockPosition, String> blocks = parseBlocks(normalized);
        SchematicBoundingBox boundingBox = computeBoundingBox(blocks);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("format", "litematic");
        metadata.put("sizeBytes", Long.toString(fileSize));
        metadata.put("lastModified", lastModified.toString());

        return new LoadedSchematic(
            normalized.toString(),
            baseName,
            "litematic",
            normalized,
            fileSize,
            lastModified,
            boundingBox,
            blocks,
            metadata
        );
    }

    private void validateFormat(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(SUPPORTED_EXTENSION)) {
            throw new IllegalArgumentException("Unsupported schematic format: " + fileName);
        }
    }

    private Map<LoadedSchematic.BlockPosition, String> parseBlocks(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        return new LitematicParser().parse(path);
    }

    private SchematicBoundingBox computeBoundingBox(Map<LoadedSchematic.BlockPosition, String> blocks) {
        if (blocks.isEmpty()) {
            return SchematicBoundingBox.singleBlockOrigin();
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (LoadedSchematic.BlockPosition position : blocks.keySet()) {
            minX = Math.min(minX, position.x());
            minY = Math.min(minY, position.y());
            minZ = Math.min(minZ, position.z());
            maxX = Math.max(maxX, position.x());
            maxY = Math.max(maxY, position.y());
            maxZ = Math.max(maxZ, position.z());
        }

        return new SchematicBoundingBox(
            new LoadedSchematic.BlockPosition(minX, minY, minZ),
            new LoadedSchematic.BlockPosition(maxX, maxY, maxZ)
        );
    }
}
