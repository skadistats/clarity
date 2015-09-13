package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2DTClass;
import skadistats.clarity.model.s2.field.FieldProperties;
import skadistats.clarity.model.s2.field.FieldType;
import skadistats.clarity.util.TextTable;

public class S2FieldReader implements FieldReader<S2DTClass> {

    public static final HuffmanTree HUFFMAN_TREE = new HuffmanTree();

    private final TextTable debugTable = new TextTable.Builder()
        .setFrame(TextTable.FRAME_COMPAT)
        .setPadding(0, 0)
        .addColumn("FP")
        .addColumn("Name")
        .addColumn("L", TextTable.Alignment.RIGHT)
        .addColumn("H", TextTable.Alignment.RIGHT)
        .addColumn("BC", TextTable.Alignment.RIGHT)
        .addColumn("Flags", TextTable.Alignment.RIGHT)
        .addColumn("Decoder")
        .addColumn("Type")
        .addColumn("Value")
        .addColumn("#", TextTable.Alignment.RIGHT)
        .addColumn("read")
        .build();

    @Override
    public int readFields(BitStream bs, S2DTClass dtClass, FieldPath[] fieldPaths, Object[] state, boolean debug) {
        try {
            if (debug) {
                debugTable.setTitle(dtClass.toString());
                debugTable.clear();
            }

            FieldPath fp = new FieldPath();
            int n = 0;
            while (true) {
                FieldOpType op = HUFFMAN_TREE.decodeOp(bs);
                if (debug) {
                    System.out.println(op);
                }
                op.execute(fp, bs);
                if (op == FieldOpType.FieldPathEncodeFinish) {
                    break;
                }
                fieldPaths[n++] = fp;
                fp = new FieldPath(fp);
            }

            for (int r = 0; r < n; r++) {
                fp = fieldPaths[r];
                Unpacker unpacker = dtClass.getUnpackerForFieldPath(fp);
                if (unpacker == null) {
                    FieldProperties f = dtClass.getFieldForFieldPath(fp).getProperties();
                    throw new RuntimeException(String.format("no unpacker for field %s with type %s!", f.getName(), f.getType()));
                }
                int offsBefore = bs.pos();
                Object data = unpacker.unpack(bs);
                dtClass.setValueForFieldPath(fp, state, data);

                if (debug) {
                    FieldProperties props = dtClass.getFieldForFieldPath(fp).getProperties();
                    FieldType type = dtClass.getTypeForFieldPath(fp);
                    debugTable.setData(r, 0, fp);
                    debugTable.setData(r, 1, dtClass.getNameForFieldPath(fp));
                    debugTable.setData(r, 2, props.getLowValue());
                    debugTable.setData(r, 3, props.getHighValue());
                    debugTable.setData(r, 4, props.getBitCount());
                    debugTable.setData(r, 5, props.getEncodeFlags() != null ? Integer.toHexString(props.getEncodeFlags()) : "-");
                    debugTable.setData(r, 6, unpacker.getClass().getSimpleName().toString());
                    debugTable.setData(r, 7, String.format("%s%s", type.toString(true), props.getEncoder() != null ? String.format(" {%s}", props.getEncoder()) : ""));
                    debugTable.setData(r, 8, data);
                    debugTable.setData(r, 9, bs.pos() - offsBefore);
                    debugTable.setData(r, 10, bs.toString(offsBefore, bs.pos()));
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
        int n = bs.readUBitVar();
        int c = 0;
        int idx = -1;
        while (c < n) {
            idx += bs.readUBitVar();
            deletions[c++] = idx;
        }
        return n;
    }

}
