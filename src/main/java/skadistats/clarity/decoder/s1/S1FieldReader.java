package skadistats.clarity.decoder.s1;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s1.ReceiveProp;
import skadistats.clarity.model.s1.S1DTClass;

public class S1FieldReader implements FieldReader<S1DTClass> {

    @Override
    public int readFields(BitStream bs, S1DTClass dtClass, FieldPath[] fieldPaths, Object[] state, boolean debug) {
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
            fieldPaths[n++] = new FieldPath(cursor);
        }
        ReceiveProp[] receiveProps = dtClass.getReceiveProps();
        for (int ci = 0; ci < n; ci++) {
            int o = fieldPaths[ci].path[0];
            state[o] = receiveProps[o].decode(bs);
        }
        return n;
    }

}
