package skadistats.clarity.decoder.s1;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.model.s1.ReceiveProp;
import skadistats.clarity.model.s1.S1DTClass;

public class S1FieldReader implements FieldReader<S1DTClass> {

    public static final int MAX_PROPERTIES = 0x3fff;

    private final int[] indices = new int[MAX_PROPERTIES];

    @Override
    public void readFields(BitStream bs, S1DTClass dtClass, Object[] state, boolean debug) {
        int cIndices = 0;
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
            indices[cIndices++] = cursor;
        }
        ReceiveProp[] receiveProps = dtClass.getReceiveProps();
        for (int ci = 0; ci < cIndices; ci++) {
            int o = indices[ci];
            state[o] = receiveProps[o].decode(bs);
        }
    }

}
