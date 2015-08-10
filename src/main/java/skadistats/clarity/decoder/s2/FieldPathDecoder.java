package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s2.FieldPath;

public class FieldPathDecoder {

    private static final HuffmanTree.Node HUFFMAN_ROOT = new HuffmanTree().buildValve();

    public static void decode(BitStream bs) {
        HuffmanTree.Node n = HUFFMAN_ROOT;
        FieldPath fp = new FieldPath();
        while (true) {
            if (n instanceof HuffmanTree.InternalNode) {
                HuffmanTree.InternalNode in = (HuffmanTree.InternalNode) n;
                n = bs.readNumericBits(1) == 0 ? in.getLeft() : in.getRight();
            } else {
                HuffmanTree.LeafNode ln = (HuffmanTree.LeafNode) n;
                System.out.println("OP: " + ln.getOp());
                ln.getOp().execute(fp, bs);
                n = HUFFMAN_ROOT;
            }
        }
    }

    public static void main(String[] args) {
    }
}
