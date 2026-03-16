package sbuild.schematic;

import java.io.IOException;
import java.nio.file.Path;

final class LitematicParser {
    private final NbtReader nbtReader;
    private final LitematicModelDecoder decoder;

    LitematicParser() {
        this(new NbtReader(), new LitematicModelDecoder());
    }

    LitematicParser(NbtReader nbtReader, LitematicModelDecoder decoder) {
        this.nbtReader = nbtReader;
        this.decoder = decoder;
    }

    LitematicModelDecoder.ParseResult parse(Path path) throws IOException {
        return decoder.decode(path, nbtReader.readRootCompound(path));
    }
}
