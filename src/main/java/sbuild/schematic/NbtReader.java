package sbuild.schematic;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

final class NbtReader {
    Map<String, Object> readRootCompound(Path path) throws IOException {
        try (InputStream fileInput = Files.newInputStream(path);
             InputStream gzipInput = new GZIPInputStream(fileInput);
             DataInputStream input = new DataInputStream(gzipInput)) {
            NbtTag root = readNamedTag(input);
            if (root.value() instanceof Map<?, ?> rootMap) {
                Map<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        out.put(key, entry.getValue());
                    }
                }
                return Map.copyOf(out);
            }
            throw new SchematicParseException(
                "INVALID_ROOT",
                "Invalid litematic root for " + path.getFileName(),
                Map.of("filePath", path.toString(), "expected", "compound", "actual", root.type().name())
            );
        }
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

    private record NbtTag(NbtType type, String name, Object value) {}

    private enum NbtType {
        END(0), BYTE(1), SHORT(2), INT(3), LONG(4), FLOAT(5), DOUBLE(6), BYTE_ARRAY(7), STRING(8), LIST(9), COMPOUND(10), INT_ARRAY(11), LONG_ARRAY(12);

        private final int id;

        NbtType(int id) {
            this.id = id;
        }

        static NbtType fromId(int id) {
            for (NbtType type : values()) if (type.id == id) return type;
            throw new SchematicParseException("UNSUPPORTED_NBT_TAG", "Unsupported NBT tag id: " + id, Map.of("actual", id));
        }
    }
}
