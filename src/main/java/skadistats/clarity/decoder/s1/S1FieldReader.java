package skadistats.clarity.decoder.s1;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.ReceiveProp;
import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.model.s1.SendProp;
import skadistats.clarity.util.TextTable;

public class S1FieldReader implements FieldReader<S1DTClass> {

    private final TextTable debugTable = new TextTable.Builder()
        .setFrame(TextTable.FRAME_COMPAT)
        .setPadding(0, 0)
        .addColumn("Idx")
        .addColumn("Name")
        .addColumn("L", TextTable.Alignment.RIGHT)
        .addColumn("H", TextTable.Alignment.RIGHT)
        .addColumn("BC", TextTable.Alignment.RIGHT)
        .addColumn("Flags", TextTable.Alignment.RIGHT)
        .addColumn("Decoder")
        .addColumn("Value")
        .addColumn("#", TextTable.Alignment.RIGHT)
        .addColumn("read")
        .build();

    @Override
    public int readFields(BitStream bs, S1DTClass dtClass, FieldPath[] fieldPaths, Object[] state, boolean debug) {
        try {
            if (debug) {
                debugTable.setTitle(dtClass.getDtName());
                debugTable.clear();
            }

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
                int offsBefore = bs.pos();
                int o = fieldPaths[ci].path[0];
                state[o] = receiveProps[o].decode(bs);

                if (debug) {
                    SendProp sp = receiveProps[o].getSendProp();
                    debugTable.setData(ci, 0, o);
                    debugTable.setData(ci, 1, receiveProps[o].getVarName());
                    debugTable.setData(ci, 2, sp.getLowValue());
                    debugTable.setData(ci, 3, sp.getHighValue());
                    debugTable.setData(ci, 4, sp.getNumBits());
                    debugTable.setData(ci, 5, PropFlag.descriptionForFlags(sp.getFlags()));
                    debugTable.setData(ci, 6, sp.getUnpacker().getClass().getSimpleName());
                    debugTable.setData(ci, 7, state[o]);
                    debugTable.setData(ci, 8, bs.pos() - offsBefore);
                    debugTable.setData(ci, 9, bs.toString(offsBefore, bs.pos()));
                }

            }
            return n;
        } finally {
            if (debug) {
                debugTable.print(System.out);
            }
        }
    }

    @Override
    public int readDeletions(BitStream bs, int indexBits, int[] deletions) {
        int n = 0;
        while (bs.readBitFlag()) {
            deletions[n++]= bs.readUBitInt(indexBits);
        }
        return n;
    }

}
