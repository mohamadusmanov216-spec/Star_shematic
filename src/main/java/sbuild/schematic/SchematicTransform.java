package sbuild.schematic;

import java.util.List;

/**
 * Иммутабельное описание трансформации схематики:
 * поворот, зеркалирование и смещение.
 */
public record SchematicTransform(
    Rotation rotation,
    Mirror mirror,
    int offsetX,
    int offsetY,
    int offsetZ
) {
    public static SchematicTransform identity() {
        return new SchematicTransform(Rotation.NONE, Mirror.NONE, 0, 0, 0);
    }

    public LoadedSchematic.BlockPosition apply(LoadedSchematic.BlockPosition position, SchematicBoundingBox bounds) {
        LoadedSchematic.BlockPosition normalized = new LoadedSchematic.BlockPosition(
            position.x() - bounds.min().x(),
            position.y() - bounds.min().y(),
            position.z() - bounds.min().z()
        );

        LoadedSchematic.BlockPosition mirrored = applyMirror(normalized, bounds);
        LoadedSchematic.BlockPosition rotated = applyRotation(mirrored, bounds);

        return new LoadedSchematic.BlockPosition(
            rotated.x() + bounds.min().x() + offsetX,
            rotated.y() + bounds.min().y() + offsetY,
            rotated.z() + bounds.min().z() + offsetZ
        );
    }

    public SchematicBoundingBox transformBounds(SchematicBoundingBox bounds) {
        List<LoadedSchematic.BlockPosition> transformedCorners = List.of(
            apply(new LoadedSchematic.BlockPosition(bounds.min().x(), bounds.min().y(), bounds.min().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.min().x(), bounds.min().y(), bounds.max().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.max().x(), bounds.min().y(), bounds.min().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.max().x(), bounds.min().y(), bounds.max().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.min().x(), bounds.max().y(), bounds.min().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.min().x(), bounds.max().y(), bounds.max().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.max().x(), bounds.max().y(), bounds.min().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.max().x(), bounds.max().y(), bounds.max().z()), bounds)
        );
        return SchematicBoundingBox.fromPositions(transformedCorners);
    }

    private LoadedSchematic.BlockPosition applyMirror(LoadedSchematic.BlockPosition position, SchematicBoundingBox bounds) {
        int maxX = bounds.sizeX() - 1;
        int maxZ = bounds.sizeZ() - 1;
        return switch (mirror) {
            case NONE -> position;
            case X -> new LoadedSchematic.BlockPosition(maxX - position.x(), position.y(), position.z());
            case Z -> new LoadedSchematic.BlockPosition(position.x(), position.y(), maxZ - position.z());
        };
    }

    private LoadedSchematic.BlockPosition applyRotation(LoadedSchematic.BlockPosition position, SchematicBoundingBox bounds) {
        int maxX = bounds.sizeX() - 1;
        int maxZ = bounds.sizeZ() - 1;
        return switch (rotation) {
            case NONE -> position;
            case CLOCKWISE_90 -> new LoadedSchematic.BlockPosition(maxZ - position.z(), position.y(), position.x());
            case CLOCKWISE_180 -> new LoadedSchematic.BlockPosition(maxX - position.x(), position.y(), maxZ - position.z());
            case CLOCKWISE_270 -> new LoadedSchematic.BlockPosition(position.z(), position.y(), maxX - position.x());
        };
    }

    public enum Rotation {
        NONE,
        CLOCKWISE_90,
        CLOCKWISE_180,
        CLOCKWISE_270
    }

    public enum Mirror {
        NONE,
        X,
        Z
    }
}
