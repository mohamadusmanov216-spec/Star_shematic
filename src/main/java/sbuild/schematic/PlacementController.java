package sbuild.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Управляет размещением схематики в мире с учетом трансформаций.
 *
 * <p>Класс отдаёт нормализованные данные для следующего этапа:
 * planner/bot смогут забирать готовые world-координаты блоков.</p>
 */
public final class PlacementController {
    private final LoadedSchematic schematic;
    private final SchematicTransform transform;
    private final Map<LoadedSchematic.BlockPosition, String> transformedBlocks;
    private final SchematicBoundingBox transformedBoundingBox;

    public PlacementController(LoadedSchematic schematic, SchematicTransform transform) {
        this.schematic = schematic;
        this.transform = transform;
        this.transformedBlocks = buildTransformedBlocks(schematic, transform);
        this.transformedBoundingBox = buildBoundingBox(this.transformedBlocks, schematic, transform);
    }

    public LoadedSchematic schematic() {
        return schematic;
    }

    public SchematicTransform transform() {
        return transform;
    }

    public Optional<String> blockAtTransformed(LoadedSchematic.BlockPosition transformedPosition) {
        return Optional.ofNullable(transformedBlocks.get(transformedPosition));
    }

    public List<Map.Entry<LoadedSchematic.BlockPosition, String>> transformedEntries() {
        return List.copyOf(transformedBlocks.entrySet());
    }

    public SchematicBoundingBox transformedBoundingBox() {
        return transformedBoundingBox;
    }

    private static Map<LoadedSchematic.BlockPosition, String> buildTransformedBlocks(
        LoadedSchematic schematic,
        SchematicTransform transform
    ) {
        List<Map.Entry<LoadedSchematic.BlockPosition, String>> transformed = new ArrayList<>();
        SchematicBoundingBox bounds = schematic.boundingBox();

        for (Map.Entry<LoadedSchematic.BlockPosition, String> entry : schematic.entries()) {
            LoadedSchematic.BlockPosition transformedPos = transform.apply(entry.getKey(), bounds);
            transformed.add(Map.entry(transformedPos, entry.getValue()));
        }

        return Map.copyOf(transformed.stream().collect(java.util.stream.Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (left, right) -> right
        )));
    }

    private static SchematicBoundingBox buildBoundingBox(
        Map<LoadedSchematic.BlockPosition, String> transformedBlocks,
        LoadedSchematic schematic,
        SchematicTransform transform
    ) {
        if (transformedBlocks.isEmpty()) {
            LoadedSchematic.BlockPosition origin = transform.apply(new LoadedSchematic.BlockPosition(0, 0, 0), schematic.boundingBox());
            return new SchematicBoundingBox(origin, origin);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (LoadedSchematic.BlockPosition position : transformedBlocks.keySet()) {
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
