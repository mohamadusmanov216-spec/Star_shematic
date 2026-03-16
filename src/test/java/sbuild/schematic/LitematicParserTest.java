package sbuild.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LitematicParserTest {
    @Test
    void readsRegionModelWithNegativeSizeAndDecodesBlocks() {
        LitematicParser parser = new LitematicParser();
        Map<String, Object> region = Map.of(
            "Position", Map.of("x", 10, "y", 5, "z", 7),
            "Size", Map.of("x", -2, "y", 1, "z", 2),
            "BlockStatePalette", List.of(
                Map.of("Name", "minecraft:air"),
                Map.of("Name", "minecraft:stone")
            ),
            "BlockStates", packedBits(2, 1, 0, 1, 0)
        );

        LitematicParser.RegionModel model = parser.readRegionModel("demo", region);
        LitematicParser.ParseAccumulator out = new LitematicParser.ParseAccumulator();
        parser.decodeRegionBlocks(model, out);

        assertEquals(3, out.blocks.size());
        assertEquals(2, model.volume().sizeX());
        assertEquals(9, model.volume().startX());
        assertEquals(2, out.airBlocks);
    }

    @Test
    void rejectsEmptyPaletteModel() {
        LitematicParser parser = new LitematicParser();
        assertThrows(IllegalArgumentException.class, () -> parser.readPaletteModel("r1", List.of()));
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
