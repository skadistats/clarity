package skadistats.clarity.decoder.s1;

import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.model.FieldPath;

public class CsGoFieldReader extends S1FieldReader{

    @Override
    protected int readIndices(BitStream bs, S1DTClass dtClass) {
        boolean nway = bs.readBitFlag();
        int n = 0;
        int cursor = -1;
        while (true) {
            if (nway && bs.readBitFlag()) {
                // 1, 1 = increment
                cursor += 1;
            } else if (nway && bs.readBitFlag()) {
                // 1, 0, 1 = new index is 3 bits
                cursor += 1 + bs.readUBitInt(3);
            } else {
                int v = bs.readUBitInt(7);
                switch (v & ( 32 | 64)) {
                    case 32:
                        v = ((v &~ 96) | (bs.readUBitInt(2) << 5));
                        break;
                    case 64:
                        v = ((v &~ 96) | (bs.readUBitInt(4) << 5));
                        break;
                    case 96:
                        v = ((v &~ 96) | (bs.readUBitInt(7) << 5));
                        break;
                }
                if (v == 0xFFF) {
                    return n;
                }
                cursor += 1 + v;
            }
            fieldPaths[n++] = new FieldPath(dtClass.getIndexMapping()[cursor]);
        }
    }

}
