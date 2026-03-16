package sbuild.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LitematicParserTest {
    @Test
    void readsRegionModelWithNegativeSizeAndDecodesBlocks() {
        LitematicModelDecoder decoder = new LitematicModelDecoder();
        Map<String, Object> region = Map.of(
            "Position", Map.of("x", 10, "y", 5, "z", 7),
            "Size", Map.of("x", -2, "y", 1, "z", 2),
            "BlockStatePalette", List.of(
                Map.of("Name", "minecraft:air"),
                Map.of("Name", "minecraft:stone")
            ),
            "BlockStates", packedBits(2, 1, 0, 1, 0)
        );

        LitematicModelDecoder.RegionModel model = decoder.readRegionModel("demo", region, Path.of("demo.litematic"));
        LitematicModelDecoder.ParseAccumulator out = new LitematicModelDecoder.ParseAccumulator();
        decoder.decodeRegionBlocks(Path.of("demo.litematic"), model, out);

        assertEquals(3, out.blocks.size());
        assertEquals(2, model.volume().sizeX());
        assertEquals(9, model.volume().startX());
        assertEquals(2, out.airBlocks);
    }

    @Test
    void rejectsTruncatedBlockStates() {
        LitematicModelDecoder decoder = new LitematicModelDecoder();
        Map<String, Object> region = Map.of(
            "Position", Map.of("x", 0, "y", 0, "z", 0),
            "Size", Map.of("x", 5, "y", 1, "z", 1),
            "BlockStatePalette", List.of(
                Map.of("Name", "minecraft:air"),
                Map.of("Name", "minecraft:stone")
            ),
            "BlockStates", new long[0]
        );

        SchematicParseException ex = assertThrows(
            SchematicParseException.class,
            () -> decoder.readRegionModel("r1", region, Path.of("demo.litematic"))
        );
        assertEquals("TRUNCATED_BLOCKSTATES", ex.reasonCode());
    }

    @Test
    void rejectsPaletteIndexOutOfBounds() {
        LitematicModelDecoder decoder = new LitematicModelDecoder();
        Map<String, Object> region = Map.of(
            "Position", Map.of("x", 0, "y", 0, "z", 0),
            "Size", Map.of("x", 1, "y", 1, "z", 1),
            "BlockStatePalette", List.of(Map.of("Name", "minecraft:air")),
            "BlockStates", packedBits(2, 1)
        );

        LitematicModelDecoder.RegionModel model = decoder.readRegionModel("r1", region, Path.of("demo.litematic"));
        SchematicParseException ex = assertThrows(
            SchematicParseException.class,
            () -> decoder.decodeRegionBlocks(Path.of("demo.litematic"), model, new LitematicModelDecoder.ParseAccumulator())
        );
        assertEquals("PALETTE_INDEX_OUT_OF_BOUNDS", ex.reasonCode());
    }

    @Test
    void rejectsMissingRequiredNumericPositionField() {
        LitematicModelDecoder decoder = new LitematicModelDecoder();
        Map<String, Object> region = Map.of(
            "Position", Map.of("x", 0, "z", 0),
            "Size", Map.of("x", 1, "y", 1, "z", 1),
            "BlockStatePalette", List.of(Map.of("Name", "minecraft:air")),
            "BlockStates", packedBits(2, 0)
        );

        SchematicParseException ex = assertThrows(
            SchematicParseException.class,
            () -> decoder.readRegionModel("r1", region, Path.of("demo.litematic"))
        );
        assertEquals("INVALID_FIELD_TYPE", ex.reasonCode());
    }

    private long[] packedBits(int bitsPerValue, int... values) {
        int bitCount = bitsPerValue * values.length;
        int longs = (bitCount + 63) / 64;
        long[] out = new long[longs];

        int bitCursor = 0;
        long mask = (1L << bitsPerValue) - 1L;
        for (int value : values) {
            int longIndex = bitCursor >>> 6;
            int offset = bitCursor & 63;
            out[longIndex] |= ((long) value & mask) << offset;
            if (offset + bitsPerValue > 64) {
                out[longIndex + 1] |= ((long) value & mask) >>> (64 - offset);
            }
            bitCursor += bitsPerValue;
        }
        return out;
    }
}
