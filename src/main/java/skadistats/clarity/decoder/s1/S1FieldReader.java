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



    @Override
    public int readFields(BitStream bs, S1DTClass dtClass, FieldPath[] fieldPaths, Object[] state, boolean debug) {
        TextTable debugTable = null;
        if (debug) {
            TextTable.Builder b = new TextTable.Builder();
            b.setTitle(dtClass.getDtName());
            b.setFrame(TextTable.FRAME_COMPAT);
            b.setPadding(0, 0);
            b.addColumn("IDX");
            b.addColumn("Name");
            b.addColumn("L", TextTable.Alignment.RIGHT);
            b.addColumn("H", TextTable.Alignment.RIGHT);
            b.addColumn("BC", TextTable.Alignment.RIGHT);
            b.addColumn("Flags", TextTable.Alignment.RIGHT);
            b.addColumn("Decoder");
            b.addColumn("Value");
            b.addColumn("#", TextTable.Alignment.RIGHT);
            b.addColumn("read");
            debugTable = b.build();
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
        if (debug) {
            debugTable.print(System.out);
        }
        return n;
    }

}
