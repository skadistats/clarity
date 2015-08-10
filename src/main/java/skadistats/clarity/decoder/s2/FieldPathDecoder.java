package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s2.FieldOpType;
import skadistats.clarity.model.s2.FieldPath;

public class FieldPathDecoder {

    public static final HuffmanTree.Node HUFFMAN_ROOT = new HuffmanTree().buildStatic();

    public static void decode(BitStream bs) {
        HuffmanTree.Node n = HUFFMAN_ROOT;
        FieldPath fp = new FieldPath();
        int offs = 0;
        while (true) {
            if (n instanceof HuffmanTree.InternalNode) {
                HuffmanTree.InternalNode in = (HuffmanTree.InternalNode) n;
                n = bs.readNumericBits(1) == 0 ? in.getLeft() : in.getRight();
                if (n == null) {
                    System.out.println(bs);
                    throw new RuntimeException("prefix with " + (bs.pos() - offs) + " bits not in tree");
                }
            } else {
                HuffmanTree.LeafNode ln = (HuffmanTree.LeafNode) n;
                ln.getOp().execute(fp, bs);
                System.out.format("%4s bits: %-30s -> %s\n", (bs.pos() - offs), ln.getOp(), fp);
                if (ln.getOp() == FieldOpType.FieldPathEncodeFinish) {
                    break;
                }
                n = HUFFMAN_ROOT;
                offs = bs.pos();
            }
        }
    }

    public static void main(String[] args) {
    }
}
