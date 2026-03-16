package sbuild.schematic;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record LoadedSchematic(
    String id,
    String name,
    String format,
    Path sourcePath,
    long fileSizeBytes,
    Instant lastModified,
    SchematicBoundingBox boundingBox,
    Map<BlockPosition, String> blocks,
    SchematicStats stats,
    Map<String, String> metadata
) {
    public LoadedSchematic {
        blocks = Map.copyOf(blocks);
        metadata = Map.copyOf(metadata);
    }

    public Optional<String> blockAt(BlockPosition position) {
        return Optional.ofNullable(blocks.get(position));
    }

    public int blockCount() {
        return blocks.size();
    }

    public Map<String, Long> requiredBlockStates() {
        Map<String, Long> counts = new java.util.HashMap<>();
        for (String state : blocks.values()) {
            if (!"minecraft:air".equals(state)) {
                counts.merge(state, 1L, Long::sum);
            }
        }
        return Map.copyOf(counts);
    }

    public Collection<Map.Entry<BlockPosition, String>> entries() {
        return List.copyOf(blocks.entrySet());
    }

    public record BlockPosition(int x, int y, int z) {
        public BlockPosition add(int dx, int dy, int dz) {
            return new BlockPosition(x + dx, y + dy, z + dz);
        }
    }

    public record SchematicStats(int regionCount, int paletteEntries, int airBlocks) {}
}
