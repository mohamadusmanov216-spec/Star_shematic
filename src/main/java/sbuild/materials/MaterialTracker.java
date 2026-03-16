package sbuild.materials;

import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Считает необходимые материалы и сравнивает их с миром/инвентарем.
 */
public final class MaterialTracker {
    private final ItemResolver itemResolver;

    public MaterialTracker(ItemResolver itemResolver) {
        this.itemResolver = itemResolver;
    }

    /**
     * Анализ по уже трансформированному размещению.
     *
     * @param placedWorldBlocks snapshot блоков мира по world-позициям
     * @param availability snapshot инвентаря/доступных ресурсов
     */
    public MaterialReport analyze(
        PlacementController placement,
        Map<LoadedSchematic.BlockPosition, String> placedWorldBlocks,
        MaterialAvailability availability
    ) {
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(placedWorldBlocks, "placedWorldBlocks");
        Objects.requireNonNull(availability, "availability");

        Map<String, Long> requiredByMaterial = new HashMap<>();
        Map<String, Long> builtByMaterial = new HashMap<>();

        for (Map.Entry<LoadedSchematic.BlockPosition, String> requiredEntry : placement.transformedEntries()) {
            String requiredBlockState = requiredEntry.getValue();
            String requiredMaterial = itemResolver.resolveItemKey(requiredBlockState);
            increment(requiredByMaterial, requiredMaterial, 1L);

            String worldBlockState = placedWorldBlocks.get(requiredEntry.getKey());
            if (worldBlockState == null) {
                continue;
            }

            String worldMaterial = itemResolver.resolveItemKey(worldBlockState);
            if (requiredMaterial.equals(worldMaterial)) {
                increment(builtByMaterial, requiredMaterial, 1L);
            }
        }

        return buildReport(requiredByMaterial, builtByMaterial, availability);
    }

    /**
     * Упрощённый анализ без проверки мира (для предварительной сметы).
     */
    public MaterialReport analyzeRequiredOnly(PlacementController placement, MaterialAvailability availability) {
        return analyze(placement, Map.of(), availability);
    }

    private MaterialReport buildReport(
        Map<String, Long> requiredByMaterial,
        Map<String, Long> builtByMaterial,
        MaterialAvailability availability
    ) {
        Map<String, MaterialRequirement> rows = new HashMap<>();
        long totalRequired = 0L;
        long totalBuilt = 0L;
        long totalRemaining = 0L;
        long totalAvailable = 0L;

        for (Map.Entry<String, Long> requiredEntry : requiredByMaterial.entrySet()) {
            String material = requiredEntry.getKey();
            long required = requiredEntry.getValue();
            long built = Math.min(required, builtByMaterial.getOrDefault(material, 0L));
            long remaining = Math.max(0L, required - built);
            long available = availability.countOf(material);

            MaterialRequirement requirement = new MaterialRequirement(
                material,
                required,
                built,
                remaining,
                available
            );

            rows.put(material, requirement);
            totalRequired += required;
            totalBuilt += built;
            totalRemaining += remaining;
            totalAvailable += available;
        }

        return new MaterialReport(rows, totalRequired, totalBuilt, totalRemaining, totalAvailable);
    }

    private void increment(Map<String, Long> target, String key, long delta) {
        target.merge(key, delta, Long::sum);
    }
}
