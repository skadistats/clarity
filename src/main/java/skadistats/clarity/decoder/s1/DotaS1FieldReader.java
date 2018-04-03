package skadistats.clarity.decoder.s1;

import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.model.FieldPath;

public class DotaS1FieldReader extends S1FieldReader{

    @Override
    protected int readIndices(BitStream bs, S1DTClass dtClass) {
        int n = 0;
        int cursor = -1;
        while (true) {
            if (bs.readBitFlag()) {
                cursor += 1;
            } else {
                int offset = bs.readVarUInt();
                if (offset == MAX_PROPERTIES) {
                    break;
                } else {
                    cursor += offset + 1;
                }
            }
            fieldPaths[n++] = new FieldPath(dtClass.getIndexMapping()[cursor]);
        }
        return n;
    }

}
