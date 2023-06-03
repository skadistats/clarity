package skadistats.clarity.io.s2;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class FieldOpHuffmanTree {

    public static final Node root;
    public static final int[][] tree;
    public static final FieldOpType[] ops = FieldOpType.values();

    static {
        root = buildTree();
        List<int[]> akku = new ArrayList<>();
        buildFixedTreeR(akku, root);
        tree = reverseTree(akku);
    }

    private static Node buildTree() {
        var queue = new PriorityQueue<Node>();
        var n = 0;
        for (var op : ops) {
            queue.offer(new LeafNode(op, n++));
        }
        while (queue.size() > 1) {
            queue.offer(new InternalNode(queue.poll(), queue.poll(), n++));
        }
        return queue.peek();
    }

    private static int buildFixedTreeR(List<int[]> akku, Node n) {
        akku.add(
            new int[] {
                (n.left  instanceof LeafNode) ? - n.left.op.ordinal() - 1 : buildFixedTreeR(akku, n.left),
                (n.right instanceof LeafNode) ? - n.right.op.ordinal() - 1 : buildFixedTreeR(akku, n.right)
            }
        );
        return akku.size() - 1;
    }

    private static int[][] reverseTree(List<int[]> akku) {
        var r = akku.size() - 1;
        var reverse = new int[r + 1][2];
        for (var i = 0; i <= r; i++) {
            for (var j = 0; j <= 1; j++) {
                var s = akku.get(r - i)[j];
                reverse[i][j] = s < 0 ? s : r - s;
            }
        }
        return reverse;
    }

    private void dump(int i, String prefix) {
        for (var s = 0; s < 2; s++) {
            if (tree[i][s] < 0) {
                System.out.println(ops[- tree[i][s] - 1] + ": " + prefix + s);
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
            var r = Integer.compare(weight, o.weight);
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
