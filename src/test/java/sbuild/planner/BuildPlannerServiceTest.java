package sbuild.planner;

import org.junit.jupiter.api.Test;
import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.schematic.SchematicBlockState;
import sbuild.schematic.SchematicBoundingBox;
import sbuild.schematic.SchematicTransform;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildPlannerServiceTest {
    @Test
    void prefersLessDependentTasksOnSameLayer() {
        LoadedSchematic.BlockPosition center = new LoadedSchematic.BlockPosition(0, 1, 0);
        LoadedSchematic.BlockPosition side = new LoadedSchematic.BlockPosition(2, 1, 0);

        Map<LoadedSchematic.BlockPosition, SchematicBlockState> blocks = new LinkedHashMap<>();
        blocks.put(center, SchematicBlockState.of("minecraft:stone", Map.of()));
        blocks.put(center.add(1, 0, 0), SchematicBlockState.of("minecraft:stone", Map.of()));
        blocks.put(center.add(-1, 0, 0), SchematicBlockState.of("minecraft:stone", Map.of()));
        blocks.put(center.add(0, 0, 1), SchematicBlockState.of("minecraft:stone", Map.of()));
        blocks.put(center.add(0, 0, -1), SchematicBlockState.of("minecraft:stone", Map.of()));
        blocks.put(side, SchematicBlockState.of("minecraft:stone", Map.of()));

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
            new LoadedSchematic.SchematicStats(1, 2, 0, blocks.size()),
            Map.of()
        );

        PlacementController placement = new PlacementController(schematic, SchematicTransform.identity());
        BuildPlannerService.BuildPlan plan = new BuildPlannerService().createPlan(placement, Map.of());
        List<LoadedSchematic.BlockPosition> ordered = plan.tasks().stream().map(BuildPlannerService.PlacementTask::position).toList();

        assertEquals(side, ordered.get(0));
    }
}
