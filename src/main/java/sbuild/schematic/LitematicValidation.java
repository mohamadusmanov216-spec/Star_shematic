package sbuild.schematic;

import java.nio.file.Path;
import java.util.Map;

final class LitematicValidation {
    LitematicModelDecoder.Vec3 requireVec3(Object value, String fieldName, String regionName, Path path) {
        if (!(value instanceof Map<?, ?> compound)) {
            throw error("MISSING_REQUIRED_FIELD", "Missing required compound field: " + fieldName, regionName, path,
                Map.of("expected", "compound{x:int,y:int,z:int}", "actual", typeOf(value), "field", fieldName));
        }

        return new LitematicModelDecoder.Vec3(
            requireInt(compound.get("x"), fieldName + ".x", regionName, path),
            requireInt(compound.get("y"), fieldName + ".y", regionName, path),
            requireInt(compound.get("z"), fieldName + ".z", regionName, path)
        );
    }

    long[] requireLongArray(Object value, String fieldName, String regionName, Path path) {
        if (value instanceof long[] longArray) {
            return longArray;
        }
        throw error("INVALID_FIELD_TYPE", "Field must be long[]: " + fieldName, regionName, path,
            Map.of("expected", "long[]", "actual", typeOf(value), "field", fieldName));
    }

    void validatePackedData(String regionName, LitematicModelDecoder.RegionVolume volume, LitematicModelDecoder.PaletteModel palette, long[] blockStates, Path path) {
        if (volume.isEmpty()) {
            throw error("EMPTY_REGION", "Region has empty size: " + regionName, regionName, path, Map.of());
        }

        int requiredLongs = (int) Math.ceil((double) ((long) volume.blockCount() * palette.bitsPerBlock()) / 64D);
        if (blockStates.length < requiredLongs) {
            throw error(
                "TRUNCATED_BLOCKSTATES",
                "Region has truncated block states: " + regionName,
                regionName,
                path,
                Map.of("expected", requiredLongs, "actual", blockStates.length)
            );
        }
    }

    private int requireInt(Object value, String fieldName, String regionName, Path path) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw error("INVALID_FIELD_TYPE", "Field must be numeric: " + fieldName, regionName, path,
            Map.of("expected", "number", "actual", typeOf(value), "field", fieldName));
    }

    SchematicParseException error(String reasonCode, String message, String regionName, Path path, Map<String, Object> details) {
        Map<String, Object> context = new java.util.LinkedHashMap<>();
        if (regionName != null) {
            context.put("regionName", regionName);
        }
        if (path != null) {
            context.put("filePath", path.toString());
        }
        context.putAll(details);
        return new SchematicParseException(reasonCode, message, context);
    }

    private String typeOf(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
