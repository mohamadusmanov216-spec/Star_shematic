package sbuild.schematic;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable in-memory model загруженной схематики.
 *
 * <p>Структура ориентирована на дальнейший planner/AI-пайплайн:
 * хранит нормализованные блоки, метаданные и границы для быстрых запросов.</p>
 */
public record LoadedSchematic(
    String id,
    String name,
    String format,
    Path sourcePath,
    long fileSizeBytes,
    Instant lastModified,
    SchematicBoundingBox boundingBox,
    Map<BlockPosition, String> blocks,
    Map<String, String> metadata
) {
    public LoadedSchematic {
        blocks = Map.copyOf(blocks);
        metadata = Map.copyOf(metadata);
    }

    public Optional<String> blockAt(BlockPosition position) {
        return Optional.ofNullable(blocks.get(position));
    }

    public boolean hasBlockAt(BlockPosition position) {
        return blocks.containsKey(position);
    }

    public int blockCount() {
        return blocks.size();
    }

    public Collection<Map.Entry<BlockPosition, String>> entries() {
        return List.copyOf(blocks.entrySet());
    }

    /**
     * Целочисленная позиция блока в локальных координатах схематики.
     */
    public record BlockPosition(int x, int y, int z) {
        public BlockPosition add(int dx, int dy, int dz) {
            return new BlockPosition(x + dx, y + dy, z + dz);
        }
    }
}
