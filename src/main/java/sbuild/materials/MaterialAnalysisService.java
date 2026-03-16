package sbuild.materials;

import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;

import java.util.Map;

/**
 * Фасад material analysis для planner/GUI/commands.
 */
public final class MaterialAnalysisService {
    private final ItemResolver itemResolver;
    private final MaterialTracker materialTracker;

    public MaterialAnalysisService() {
        this.itemResolver = new ItemResolver();
        this.materialTracker = new MaterialTracker(itemResolver);
    }

    public void initialize() {
        // Reserved for future cache warmup / external providers.
    }

    public MaterialReport analyze(
        PlacementController placement,
        Map<LoadedSchematic.BlockPosition, String> placedWorldBlocks,
        MaterialAvailability availability
    ) {
        return materialTracker.analyze(placement, placedWorldBlocks, availability);
    }

    public MaterialReport analyzeRequiredOnly(PlacementController placement, MaterialAvailability availability) {
        return materialTracker.analyzeRequiredOnly(placement, availability);
    }

    public ItemResolver itemResolver() {
        return itemResolver;
    }
}
