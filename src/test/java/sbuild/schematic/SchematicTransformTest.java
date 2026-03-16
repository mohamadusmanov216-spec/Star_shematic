package sbuild.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchematicTransformTest {
    @Test
    void mirrorAndRotationCombinationsKeepPositionsInsideTransformedBounds() {
        SchematicBoundingBox bounds = new SchematicBoundingBox(
            new LoadedSchematic.BlockPosition(10, 2, -5),
            new LoadedSchematic.BlockPosition(12, 4, -2)
        );

        for (SchematicTransform.Mirror mirror : SchematicTransform.Mirror.values()) {
            for (SchematicTransform.Rotation rotation : SchematicTransform.Rotation.values()) {
                SchematicTransform transform = new SchematicTransform(rotation, mirror, 3, -1, 7);
                SchematicBoundingBox transformedBounds = transform.transformBounds(bounds);

                Set<LoadedSchematic.BlockPosition> unique = new HashSet<>();
                for (int x = bounds.min().x(); x <= bounds.max().x(); x++) {
                    for (int y = bounds.min().y(); y <= bounds.max().y(); y++) {
                        for (int z = bounds.min().z(); z <= bounds.max().z(); z++) {
                            LoadedSchematic.BlockPosition mapped = transform.apply(new LoadedSchematic.BlockPosition(x, y, z), bounds);
                            unique.add(mapped);
                            assertTrue(transformedBounds.contains(mapped), "point should stay in transformed bounds for " + mirror + "+" + rotation);
                        }
                    }
                }

                int sourceVolume = bounds.sizeX() * bounds.sizeY() * bounds.sizeZ();
                assertEquals(sourceVolume, unique.size(), "transform must remain bijective for " + mirror + "+" + rotation);
            }
        }
    }
}
