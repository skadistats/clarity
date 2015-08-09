package skadistats.clarity.decoder.s2;

import skadistats.clarity.model.s2.FieldOpType;

import java.util.PriorityQueue;

public class FieldPathDecoder {

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
    }


    public static void main(String[] args) {
    }
}
