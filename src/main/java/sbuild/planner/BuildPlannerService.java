package sbuild.planner;

import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.schematic.SchematicBlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds deterministic placement plans from transformed schematic blocks.
 */
public final class BuildPlannerService {
    public BuildPlan createPlan(
        PlacementController placement,
        Map<LoadedSchematic.BlockPosition, String> worldStates
    ) {
        List<PlacementTask> tasks = new ArrayList<>();
        int skippedAlreadyCorrect = 0;

        for (Map.Entry<LoadedSchematic.BlockPosition, SchematicBlockState> entry : placement.transformedEntries()) {
            String required = entry.getValue().key();
            if (entry.getValue().isAir()) {
                continue;
            }

            String world = worldStates.get(entry.getKey());
            if (required.equals(world)) {
                skippedAlreadyCorrect++;
                continue;
            }
            tasks.add(new PlacementTask(entry.getKey(), required, world));
        }

        tasks.sort(Comparator
            .comparingInt((PlacementTask t) -> t.position().y())
            .thenComparingInt(t -> t.position().x())
            .thenComparingInt(t -> t.position().z()));

        return new BuildPlan(List.copyOf(tasks), skippedAlreadyCorrect);
    }

    public record PlacementTask(
        LoadedSchematic.BlockPosition position,
        String requiredState,
        String worldState
    ) {
    }

    public record BuildPlan(List<PlacementTask> tasks, int skippedAlreadyCorrect) {
        public BuildPlan {
            tasks = List.copyOf(tasks);
        }

        public boolean isEmpty() {
            return tasks.isEmpty();
        }
    }
}
