package skadistats.clarity.decoder.s1;

import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.util.TextTable;

import java.util.Arrays;
import java.util.function.Consumer;

public abstract class S1FieldReader extends FieldReader<S1DTClass> {

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

    protected abstract int readIndices(BitStream bs, S1DTClass dtClass);

    @Override
    public int readFields(BitStream bs, S1DTClass dtClass, EntityState state, Consumer<FieldPath> fieldPathConsumer, boolean debug) {
        try {
            if (debug) {
                debugTable.setTitle(dtClass.getDtName());
                debugTable.clear();
            }

            int n = readIndices(bs, dtClass);

            ReceiveProp[] receiveProps = dtClass.getReceiveProps();
            for (int ci = 0; ci < n; ci++) {
                int offsBefore = bs.pos();
                int o = fieldPaths[ci].path[0];
                state.setValueForFieldPath(fieldPaths[ci], receiveProps[o].decode(bs));

                if (debug) {
                    SendProp sp = receiveProps[o].getSendProp();
                    Object subState = state.getValueForFieldPath(fieldPaths[ci]);
                    debugTable.setData(ci, 0, o);
                    debugTable.setData(ci, 1, receiveProps[o].getVarName());
                    debugTable.setData(ci, 2, sp.getLowValue());
                    debugTable.setData(ci, 3, sp.getHighValue());
                    debugTable.setData(ci, 4, sp.getNumBits());
                    debugTable.setData(ci, 5, PropFlag.descriptionForFlags(sp.getFlags()));
                    debugTable.setData(ci, 6, sp.getUnpacker().getClass().getSimpleName());
                    debugTable.setData(ci, 7, subState.getClass().isArray() ? Arrays.toString((Object[]) subState) : subState);
                    debugTable.setData(ci, 8, bs.pos() - offsBefore);
                    debugTable.setData(ci, 9, bs.toString(offsBefore, bs.pos()));
                }

            }
            if (fieldPathConsumer != null) {
                for (int i = 0; i < n; i++) {
                    fieldPathConsumer.accept(fieldPaths[i]);
                }
            }
            return n;
        } finally {
            if (debug) {
                debugTable.print(DEBUG_STREAM);
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
