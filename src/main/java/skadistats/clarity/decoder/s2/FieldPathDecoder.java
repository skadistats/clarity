package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s2.FieldOpType;
import skadistats.clarity.model.s2.FieldPath;

import java.util.PriorityQueue;

public class FieldPathDecoder {

    private static final Node HUFFMAN_ROOT;

    public static void decode(BitStream bs) {
        Node n = HUFFMAN_ROOT;
        FieldPath fp = new FieldPath();
        while (true) {
            if (n instanceof InternalNode) {
                InternalNode in = (InternalNode) n;
                n = bs.readNumericBits(1) == 0 ? in.getLeft() : in.getRight();
            } else {
                LeafNode ln = (LeafNode) n;
                System.out.println("OP: " + ln.getOp());
                ln.getOp().execute(fp, bs);
                n = HUFFMAN_ROOT;
            }
        }
    }

    static {
        PriorityQueue<Node> queue = new PriorityQueue<>();
        int n = 1;
        for (FieldOpType op : FieldOpType.values()) {
            LeafNode newLeaf = new LeafNode(op, n++);
            queue.offer(newLeaf);
        }
        while (queue.size() > 1) {
            queue.offer(new InternalNode(queue.poll(), queue.poll(), n++));
        }

        HUFFMAN_ROOT = queue.peek();
        System.out.println(new HuffmanGraph(queue.peek()).generate());
    }

    static abstract class Node implements Comparable<Node> {
        private final int weight;
        private final int num;
        public Node(int weight, int num) {
            this.weight = weight;
            this.num = num;
        }
        public int getWeight() {
            return weight;
        }
        public int getNum() {
            return num;
        }
        @Override
        public int compareTo(Node o) {
            int r = Integer.compare(weight, o.weight);
            return r != 0 ? r : Integer.compare(num, o.num);
        }
    }

    static class LeafNode extends Node {
        private final FieldOpType op;
        public LeafNode(FieldOpType op, int num) {
            super(op.getWeight(), num);
            this.op = op;
        }
        public FieldOpType getOp() {
            return op;
        }
        @Override
        public String toString() {
            return String.format("[%s]", op.toString());
        }
    }

    static class InternalNode extends Node {
        private final Node left;
        private final Node right;
        public InternalNode(Node left, Node right, int num) {
            super(left.weight + right.weight, num);
            this.left = left;
            this.right = right;
        }
        public Node getLeft() {
            return left;
        }
        public Node getRight() {
            return right;
        }
        @Override
        public String toString() {
            return String.format("(%s)", getWeight());
        }
    }


    public static void main(String[] args) {
    }
}
