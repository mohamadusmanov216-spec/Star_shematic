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
    Map<BlockPosition, SchematicBlockState> blocks,
    SchematicStats stats,
    Map<String, String> metadata
) {
    public LoadedSchematic {
        blocks = Map.copyOf(blocks);
        metadata = Map.copyOf(metadata);
    }

    public Optional<SchematicBlockState> blockAt(BlockPosition position) {
        return Optional.ofNullable(blocks.get(position));
    }

    public Optional<String> blockStateKeyAt(BlockPosition position) {
        return blockAt(position).map(SchematicBlockState::key);
    }

    public int blockCount() {
        return blocks.size();
    }

    public Map<SchematicBlockState, Long> requiredBlockStates() {
        Map<SchematicBlockState, Long> counts = new java.util.HashMap<>();
        for (SchematicBlockState state : blocks.values()) {
            if (!state.isAir()) {
                counts.merge(state, 1L, Long::sum);
            }
        }
        return Map.copyOf(counts);
    }

    public Map<String, Long> requiredBlockStateKeys() {
        Map<String, Long> byKey = new java.util.HashMap<>();
        for (Map.Entry<SchematicBlockState, Long> entry : requiredBlockStates().entrySet()) {
            byKey.put(entry.getKey().key(), entry.getValue());
        }
        return Map.copyOf(byKey);
    }

    public Collection<Map.Entry<BlockPosition, SchematicBlockState>> entries() {
        return List.copyOf(blocks.entrySet());
    }

    public record BlockPosition(int x, int y, int z) {
        public BlockPosition add(int dx, int dy, int dz) {
            return new BlockPosition(x + dx, y + dy, z + dz);
        }
    }

    public record SchematicStats(int regionCount, int paletteEntries, int airBlocks, int solidBlocks) {}
}
