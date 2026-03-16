package sbuild.schematic;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

final class LitematicParser {
    ParseResult parse(Path path) throws IOException {
        try (InputStream fileInput = Files.newInputStream(path);
             InputStream gzipInput = new GZIPInputStream(fileInput);
             DataInputStream input = new DataInputStream(gzipInput)) {

            NbtTag root = readNamedTag(input);
            if (!(root.value() instanceof Map<?, ?> rootMap)) {
                throw new IllegalArgumentException("Invalid litematic root for " + path.getFileName());
            }

            Map<String, String> metadata = readMetadata(rootMap.get("Metadata"));
            Object regionsRaw = rootMap.get("Regions");
            if (!(regionsRaw instanceof Map<?, ?> regionsMap) || regionsMap.isEmpty()) {
                throw new IllegalArgumentException("Litematic has no regions: " + path.getFileName());
            }

            ParseAccumulator acc = new ParseAccumulator();
            for (Map.Entry<?, ?> regionEntry : regionsMap.entrySet()) {
                if (regionEntry.getValue() instanceof Map<?, ?> region) {
                    readRegion(region, acc);
                    acc.regionCount++;
                }
            }

            if (acc.blocks.isEmpty()) {
                throw new IllegalArgumentException("Litematic contains zero block states: " + path.getFileName());
            }

            return new ParseResult(
                Map.copyOf(acc.blocks),
                metadata,
                new LoadedSchematic.SchematicStats(acc.regionCount, acc.paletteEntries, acc.airBlocks)
            );
        }
    }

    private void readRegion(Map<?, ?> region, ParseAccumulator out) {
        Vec3 pos = readVec3(region.get("Position"));
        Vec3 size = readVec3(region.get("Size"));

        int sizeX = Math.abs(size.x());
        int sizeY = Math.abs(size.y());
        int sizeZ = Math.abs(size.z());
        if (sizeX == 0 || sizeY == 0 || sizeZ == 0) {
            return;
        }

        int startX = size.x() >= 0 ? pos.x() : pos.x() + size.x() + 1;
        int startY = size.y() >= 0 ? pos.y() : pos.y() + size.y() + 1;
        int startZ = size.z() >= 0 ? pos.z() : pos.z() + size.z() + 1;

        List<String> palette = readPalette(region.get("BlockStatePalette"));
        long[] blockStates = readLongArray(region.get("BlockStates"));
        if (palette.isEmpty() || blockStates.length == 0) {
            return;
        }
        out.paletteEntries += palette.size();

        int volume = sizeX * sizeY * sizeZ;
        int bitsPerBlock = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, palette.size() - 1)));

        for (int index = 0; index < volume; index++) {
            int paletteIndex = readPackedValue(blockStates, index, bitsPerBlock);
            if (paletteIndex < 0 || paletteIndex >= palette.size()) {
                continue;
            }

            int x = index % sizeX;
            int z = (index / sizeX) % sizeZ;
            int y = index / (sizeX * sizeZ);

            String blockState = palette.get(paletteIndex);
            if ("minecraft:air".equals(blockState)) {
                out.airBlocks++;
                continue;
            }

            LoadedSchematic.BlockPosition worldPos = new LoadedSchematic.BlockPosition(startX + x, startY + y, startZ + z);
            out.blocks.put(worldPos, blockState);
        }
    }

    private int readPackedValue(long[] data, int index, int bitsPerValue) {
        int bitIndex = index * bitsPerValue;
        int startLong = bitIndex >>> 6;
        int startOffset = bitIndex & 63;
        if (startLong >= data.length) {
            return -1;
        }

        long value = data[startLong] >>> startOffset;
        int endOffset = startOffset + bitsPerValue;
        if (endOffset > 64 && (startLong + 1) < data.length) {
            value |= data[startLong + 1] << (64 - startOffset);
        }

        long mask = (1L << bitsPerValue) - 1L;
        return (int) (value & mask);
    }

    private List<String> readPalette(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<String> palette = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> state)) continue;
            Object nameRaw = state.get("Name");
            if (!(nameRaw instanceof String name)) continue;
            Object propertiesRaw = state.get("Properties");
            Map<String, String> props = propertiesRaw instanceof Map<?, ?> propsMap ? readProperties(propsMap) : Map.of();
            palette.add(formatState(name, props));
        }
        return List.copyOf(palette);
    }

    private Map<String, String> readProperties(Map<?, ?> properties) {
        Map<String, String> out = new TreeMap<>();
        for (Map.Entry<?, ?> entry : properties.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
                out.put(key, value);
            }
        }
        return Map.copyOf(out);
    }

    private String formatState(String blockName, Map<String, String> properties) {
        if (properties.isEmpty()) return blockName;
        String props = properties.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((left, right) -> left + "," + right)
            .orElse("");
        return blockName + "[" + props + "]";
    }

    private long[] readLongArray(Object value) {
        return value instanceof long[] longArray ? longArray : new long[0];
    }

    private Vec3 readVec3(Object value) {
        if (!(value instanceof Map<?, ?> compound)) {
            return new Vec3(0, 0, 0);
        }
        return new Vec3(asInt(compound.get("x")), asInt(compound.get("y")), asInt(compound.get("z")));
    }

    private Map<String, String> readMetadata(Object value) {
        if (!(value instanceof Map<?, ?> metadataRaw)) {
            return Map.of();
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : metadataRaw.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() != null) {
                metadata.put(key, String.valueOf(entry.getValue()));
            }
        }
        return Map.copyOf(metadata);
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private NbtTag readNamedTag(DataInputStream in) throws IOException {
        byte typeId = in.readByte();
        if (typeId == 0) return new NbtTag(NbtType.END, "", null);
        String name = in.readUTF();
        Object payload = readPayload(in, NbtType.fromId(typeId));
        return new NbtTag(NbtType.fromId(typeId), name, payload);
    }

    private Object readPayload(DataInputStream in, NbtType type) throws IOException {
        return switch (type) {
            case END -> null;
            case BYTE -> in.readByte();
            case SHORT -> in.readShort();
            case INT -> in.readInt();
            case LONG -> in.readLong();
            case FLOAT -> in.readFloat();
            case DOUBLE -> in.readDouble();
            case BYTE_ARRAY -> {
                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                yield data;
            }
            case STRING -> in.readUTF();
            case LIST -> readList(in);
            case COMPOUND -> readCompound(in);
            case INT_ARRAY -> {
                int len = in.readInt();
                int[] data = new int[len];
                for (int i = 0; i < len; i++) data[i] = in.readInt();
                yield data;
            }
            case LONG_ARRAY -> {
                int len = in.readInt();
                long[] data = new long[len];
                for (int i = 0; i < len; i++) data[i] = in.readLong();
                yield data;
            }
        };
    }

    private List<Object> readList(DataInputStream in) throws IOException {
        NbtType elementType = NbtType.fromId(in.readByte());
        int len = in.readInt();
        List<Object> out = new ArrayList<>(len);
        for (int i = 0; i < len; i++) out.add(readPayload(in, elementType));
        return out;
    }

    private Map<String, Object> readCompound(DataInputStream in) throws IOException {
        Map<String, Object> out = new LinkedHashMap<>();
        while (true) {
            byte typeId = in.readByte();
            NbtType type = NbtType.fromId(typeId);
            if (type == NbtType.END) break;
            String key = in.readUTF();
            Object value = readPayload(in, type);
            out.put(key, value);
        }
        return out;
    }

    record ParseResult(Map<LoadedSchematic.BlockPosition, String> blocks, Map<String, String> metadata, LoadedSchematic.SchematicStats stats) {}

    private static final class ParseAccumulator {
        private final Map<LoadedSchematic.BlockPosition, String> blocks = new LinkedHashMap<>();
        private int regionCount;
        private int paletteEntries;
        private int airBlocks;
    }
    private record NbtTag(NbtType type, String name, Object value) {}

    private enum NbtType {
        END(0), BYTE(1), SHORT(2), INT(3), LONG(4), FLOAT(5), DOUBLE(6), BYTE_ARRAY(7), STRING(8), LIST(9), COMPOUND(10), INT_ARRAY(11), LONG_ARRAY(12);
        private final int id;
        NbtType(int id) { this.id = id; }
        static NbtType fromId(int id) {
            for (NbtType type : values()) if (type.id == id) return type;
            throw new IllegalArgumentException("Unsupported NBT tag id: " + id);
        }
    }

    private record Vec3(int x, int y, int z) {}
}
