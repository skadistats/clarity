package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.bitstream.BitStream;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class HuffmanTree {

    final Node root;
    private final int[][] tree;

    public HuffmanTree() {
        root = buildTree();

        List<int[]> akku = new ArrayList<>();
        buildFixedTreeR(akku, root);

        tree = reverseTree(akku);

        //dump(0, "");
    }

    public FieldOpType decodeOp(BitStream bs) {
        int i = 0;
        do {
            i = tree[i][bs.readBit()];
        } while (i >= 0);
        return FieldOpType.values()[- i - 1];
    }

    private Node buildTree() {
        PriorityQueue<Node> queue = new PriorityQueue<>();
        int n = 0;
        for (FieldOpType op : FieldOpType.values()) {
            queue.offer(new LeafNode(op, n++));
        }
        while (queue.size() > 1) {
            queue.offer(new InternalNode(queue.poll(), queue.poll(), n++));
        }
        return queue.peek();
    }

    private int buildFixedTreeR(List<int[]> akku, Node n) {
        akku.add(
            new int[] {
                (n.left  instanceof LeafNode) ? - n.left.op.ordinal() - 1 : buildFixedTreeR(akku, n.left),
                (n.right instanceof LeafNode) ? - n.right.op.ordinal() - 1 : buildFixedTreeR(akku, n.right)
            }
        );
        return akku.size() - 1;
    }

    private int[][] reverseTree(List<int[]> akku) {
        int r = akku.size() - 1;
        int[][] reverse = new int[r + 1][2];
        for (int i = 0; i <= r; i++) {
            for (int j = 0; j <= 1; j++) {
                int s = akku.get(r - i)[j];
                reverse[i][j] = s < 0 ? s : r - s;
            }
        }
        return reverse;
    }

    private void dump(int i, String prefix) {
        for (int s = 0; s < 2; s++) {
            if (tree[i][s] < 0) {
                System.out.println(FieldOpType.values()[- tree[i][s] - 1] + ": " + prefix + s);
            } else {
                dump(tree[i][s], prefix + s);
            }
        }
    }

    static abstract class Node implements Comparable<Node> {
        final int weight;
        final int num;
        FieldOpType op;
        Node left;
        Node right;
        public Node(int weight, int num) {
            this.weight = weight;
            this.num = num;
        }
        @Override
        public int compareTo(Node o) {
            int r = Integer.compare(weight, o.weight);
            return r != 0 ? r : Integer.compare(o.num, num);
        }
    }

    static class LeafNode extends Node {
        public LeafNode(FieldOpType op, int num) {
            super(Math.max(op.getWeight(), 1), num);
            this.op = op;
        }
        @Override
        public String toString() {
            return String.format("[%s]", op.toString());
        }
    }

    static class InternalNode extends Node {
        public InternalNode(Node left, Node right, int num) {
            super(left.weight + right.weight, num);
            this.left = left;
            this.right = right;
        }
        @Override
        public String toString() {
            return String.format("(%s)", weight);
        }
    }


}
