package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s2.FieldOpType;
import skadistats.clarity.model.s2.FieldPath;

import java.util.ArrayList;
import java.util.List;

public class FieldPathDecoder {

    public static final HuffmanTree HUFFMAN_TREE = new HuffmanTree();

    public static List<FieldPath> decode(BitStream bs) {
        ArrayList<FieldPath> fieldPaths = new ArrayList<>();
        FieldPath fp = new FieldPath();
        int offs = 0;
        while (true) {
            FieldOpType op = HUFFMAN_TREE.decodeOp(bs);
            op.execute(fp, bs);
            //System.out.format("%4s bits: %-30s -> %s\n", (bs.pos() - offs), op, fp);
            if (op == FieldOpType.FieldPathEncodeFinish) {
                break;
            }
            offs = bs.pos();
            fieldPaths.add(fp);
            fp = new FieldPath(fp);
        }
        return fieldPaths;
    }

}
