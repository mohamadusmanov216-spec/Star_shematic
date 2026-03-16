package sbuild.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LoadedSchematicTest {
    @Test
    void buildsDomainIndexesForLayersAndStateKeys() {
        SchematicBlockState stone = SchematicBlockState.of("minecraft:stone", Map.of());
        SchematicBlockState oak = SchematicBlockState.of("minecraft:oak_planks", Map.of());

        Map<LoadedSchematic.BlockPosition, SchematicBlockState> blocks = new LinkedHashMap<>();
        blocks.put(new LoadedSchematic.BlockPosition(0, 10, 0), stone);
        blocks.put(new LoadedSchematic.BlockPosition(1, 10, 0), stone);
        blocks.put(new LoadedSchematic.BlockPosition(0, 11, 0), oak);

        LoadedSchematic schematic = new LoadedSchematic(
            "id",
            "name",
            "litematic",
            Path.of("demo.litematic"),
            1L,
            Instant.now(),
            SchematicBoundingBox.fromPositions(blocks.keySet()),
            blocks,
            null,
            new LoadedSchematic.SchematicStats(1, 2, 0, 3),
            Map.of()
        );

        assertEquals(2, schematic.layerEntries(10).size());
        assertEquals(1, schematic.layerEntries(11).size());
        assertEquals(2L, schematic.requiredBlockStateKeys().get(stone.key()));
        assertEquals(1, schematic.positionsForStateKey(oak.key()).size());
    }
}
