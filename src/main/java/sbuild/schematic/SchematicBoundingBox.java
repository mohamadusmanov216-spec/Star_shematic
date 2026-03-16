package sbuild.schematic;

/**
 * Неизменяемый AABB схематики в локальных координатах.
 */
public record SchematicBoundingBox(
    LoadedSchematic.BlockPosition min,
    LoadedSchematic.BlockPosition max
) {
    public static SchematicBoundingBox singleBlockOrigin() {
        LoadedSchematic.BlockPosition origin = new LoadedSchematic.BlockPosition(0, 0, 0);
        return new SchematicBoundingBox(origin, origin);
    }

    public int sizeX() {
        return (max.x() - min.x()) + 1;
    }

    public int sizeY() {
        return (max.y() - min.y()) + 1;
    }

    public int sizeZ() {
        return (max.z() - min.z()) + 1;
    }

    public boolean contains(LoadedSchematic.BlockPosition position) {
        return position.x() >= min.x() && position.x() <= max.x()
            && position.y() >= min.y() && position.y() <= max.y()
            && position.z() >= min.z() && position.z() <= max.z();
    }
}
