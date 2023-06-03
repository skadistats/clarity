package skadistats.clarity.io.s1;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.s1.S1FieldPath;

public class DotaS1FieldReader extends S1FieldReader {

    @Override
    protected int readIndices(BitStream bs, S1DTClass dtClass) {
        var n = 0;
        var cursor = -1;
        while (true) {
            if (bs.readBitFlag()) {
                cursor += 1;
            } else {
                var offset = bs.readVarUInt();
                if (offset == MAX_PROPERTIES) {
                    break;
                } else {
                    cursor += offset + 1;
                }
            }
            fieldPaths[n++] = new S1FieldPath(dtClass.getIndexMapping()[cursor]);
        }
        return n;
    }

}
