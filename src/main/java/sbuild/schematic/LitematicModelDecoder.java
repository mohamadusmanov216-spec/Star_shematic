package sbuild.schematic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class LitematicModelDecoder {
    private final LitematicValidation validation;

    LitematicModelDecoder() {
        this(new LitematicValidation());
    }

    LitematicModelDecoder(LitematicValidation validation) {
        this.validation = validation;
    }

    ParseResult decode(Path path, Map<String, Object> rootMap) {
        Map<String, String> metadata = readMetadata(rootMap.get("Metadata"));
        List<RegionModel> regions = extractRegions(rootMap.get("Regions"), path);

        ParseAccumulator acc = new ParseAccumulator();
        for (RegionModel region : regions) {
            decodeRegionBlocks(path, region, acc);
            acc.regionCount++;
        }

        if (acc.blocks.isEmpty()) {
            throw validation.error("ZERO_NON_AIR_BLOCKS", "Litematic contains zero non-air blocks: " + path.getFileName(), null, path, Map.of());
        }

        return new ParseResult(
            Map.copyOf(acc.blocks),
            metadata,
            new LoadedSchematic.SchematicStats(acc.regionCount, acc.paletteEntries, acc.airBlocks, acc.blocks.size())
        );
    }

    List<RegionModel> extractRegions(Object regionsRaw, Path path) {
        if (!(regionsRaw instanceof Map<?, ?> regionsMap) || regionsMap.isEmpty()) {
            throw validation.error("MISSING_REGIONS", "Litematic has no regions", null, path, Map.of());
        }

        List<RegionModel> regions = new ArrayList<>();
        for (Map.Entry<?, ?> regionEntry : regionsMap.entrySet()) {
            if (!(regionEntry.getValue() instanceof Map<?, ?> regionRaw)) {
                continue;
            }
            regions.add(readRegionModel(regionEntry.getKey(), regionRaw, path));
        }

        if (regions.isEmpty()) {
            throw validation.error("MALFORMED_REGIONS", "Litematic regions are malformed", null, path, Map.of());
        }

        return List.copyOf(regions);
    }

    RegionModel readRegionModel(Object regionNameValue, Map<?, ?> region, Path path) {
        String regionName = String.valueOf(regionNameValue);
        Vec3 position = validation.requireVec3(region.get("Position"), "Position", regionName, path);
        Vec3 size = validation.requireVec3(region.get("Size"), "Size", regionName, path);
        RegionVolume volume = RegionVolume.from(position, size);
        PaletteModel palette = readPaletteModel(regionName, region.get("BlockStatePalette"), path);
        long[] blockStates = validation.requireLongArray(region.get("BlockStates"), "BlockStates", regionName, path);

        validation.validatePackedData(regionName, volume, palette, blockStates, path);
        return new RegionModel(regionName, volume, palette, blockStates);
    }

    PaletteModel readPaletteModel(String regionName, Object value, Path path) {
        if (!(value instanceof List<?> list)) {
            throw validation.error("INVALID_PALETTE", "Region has invalid palette: " + regionName, regionName, path,
                Map.of("expected", "list", "actual", value == null ? "null" : value.getClass().getSimpleName()));
        }

        List<SchematicBlockState> entries = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> state)) {
                continue;
            }
            Object nameRaw = state.get("Name");
            if (!(nameRaw instanceof String name)) {
                continue;
            }
            Object propertiesRaw = state.get("Properties");
            Map<String, String> props = propertiesRaw instanceof Map<?, ?> propsMap ? readProperties(propsMap) : Map.of();
            entries.add(SchematicBlockState.of(name, props));
        }

        if (entries.isEmpty()) {
            throw validation.error("EMPTY_PALETTE", "Region has empty palette: " + regionName, regionName, path, Map.of());
        }

        return new PaletteModel(List.copyOf(entries));
    }

    void decodeRegionBlocks(Path path, RegionModel region, ParseAccumulator out) {
        out.paletteEntries += region.palette().size();

        for (int index = 0; index < region.volume().blockCount(); index++) {
            int paletteIndex = readPackedValue(region.blockStates(), index, region.palette().bitsPerBlock());
            if (paletteIndex < 0 || paletteIndex >= region.palette().size()) {
                throw validation.error(
                    "PALETTE_INDEX_OUT_OF_BOUNDS",
                    "Palette index out of range in region: " + region.name(),
                    region.name(),
                    path,
                    Map.of("expected", "0.." + (region.palette().size() - 1), "actual", paletteIndex)
                );
            }

            LoadedSchematic.BlockPosition worldPos = region.volume().worldPosition(index);
            SchematicBlockState blockState = region.palette().entry(paletteIndex);
            if (blockState.isAir()) {
                out.airBlocks++;
                continue;
            }
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

        long mask = bitsPerValue == 64 ? -1L : (1L << bitsPerValue) - 1L;
        return (int) (value & mask);
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

    record ParseResult(Map<LoadedSchematic.BlockPosition, SchematicBlockState> blocks, Map<String, String> metadata, LoadedSchematic.SchematicStats stats) {}

    static final class ParseAccumulator {
        final Map<LoadedSchematic.BlockPosition, SchematicBlockState> blocks = new LinkedHashMap<>();
        int regionCount;
        int paletteEntries;
        int airBlocks;
    }

    record RegionModel(String name, RegionVolume volume, PaletteModel palette, long[] blockStates) {}

    record PaletteModel(List<SchematicBlockState> entries) {
        int size() {
            return entries.size();
        }

        int bitsPerBlock() {
            return Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, entries.size() - 1)));
        }

        SchematicBlockState entry(int index) {
            return entries.get(index);
        }
    }

    record RegionVolume(int startX, int startY, int startZ, int sizeX, int sizeY, int sizeZ) {
        static RegionVolume from(Vec3 position, Vec3 size) {
            int sx = Math.abs(size.x());
            int sy = Math.abs(size.y());
            int sz = Math.abs(size.z());

            int startX = size.x() >= 0 ? position.x() : position.x() + size.x() + 1;
            int startY = size.y() >= 0 ? position.y() : position.y() + size.y() + 1;
            int startZ = size.z() >= 0 ? position.z() : position.z() + size.z() + 1;
            return new RegionVolume(startX, startY, startZ, sx, sy, sz);
        }

        boolean isEmpty() {
            return sizeX == 0 || sizeY == 0 || sizeZ == 0;
        }

        int blockCount() {
            return sizeX * sizeY * sizeZ;
        }

        LoadedSchematic.BlockPosition worldPosition(int index) {
            int x = index % sizeX;
            int z = (index / sizeX) % sizeZ;
            int y = index / (sizeX * sizeZ);
            return new LoadedSchematic.BlockPosition(startX + x, startY + y, startZ + z);
        }
    }

    record Vec3(int x, int y, int z) {}
}
