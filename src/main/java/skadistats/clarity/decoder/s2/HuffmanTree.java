package skadistats.clarity.decoder.s2;

import skadistats.clarity.model.s2.FieldOpType;

import java.util.PriorityQueue;

public class HuffmanTree {

    private final Node[] nodes = new Node[200];
    private int count = 0;
    private int num = 1;

    public Node build() {
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
        return queue.peek();
    }


    public Node buildValve() {
        for (FieldOpType op : FieldOpType.values()) {
            add(new LeafNode(op, num++));
        }
        Node node1, node2;
        while (true) {
            node1 = nodes[0];
            if (count < 2) break;
            node2 = nodes[0];
            if (count != 1) {
                node2 = nodes[count - 1];
                nodes[0] = node2;
            }
            int lastIdx = count - 1;
            if (lastIdx >= 2) {
                magicShuffle(((count + ((count + 1) >> 31) + 1) >> 1) - 1, count - 1);
                node2 = nodes[0];
            }
            if (lastIdx > 0) {
                int preLastIdx = lastIdx - 1;
                if (preLastIdx > 0) {
                    nodes[0] = nodes[preLastIdx];
                    if (preLastIdx >= 2) {
                        magicShuffle(preLastIdx / 2, preLastIdx);
                    }
                    lastIdx = preLastIdx;
                } else {
                    lastIdx = 0;
                }
            }
            count = lastIdx;
            add(new InternalNode(node1, node2, num++));
        }

        System.out.println(new HuffmanGraph(node1).generate());
        return node1;
    }

    private void add(Node node){
        nodes[count] = node;
        if (count != 0) {
            int i = count;
            int j;
            do {
                j = i;
                i = ((i + ((i + 1) >> 31) + 1) >> 1) - 1;
                if (nodeLess(nodes[j], nodes[i])) break;
                swap(i, j);
            } while (i != 0);
        }
        count++;
    }

    private void magicShuffle(int middle, int lastIdx) {
        int left, right, pos;
        pos = 0;
        do {
            right = 2 * pos + 1;
            left = pos;
            if (right < lastIdx) {
                if (!nodeLess(nodes[pos], nodes[right])) {
                    right = pos;
                }
                left = right;
            }
            right = 2 * pos + 2;
            if (right < lastIdx && nodeLess(nodes[left], nodes[right])) {
                left = right;
            }
            if (left == pos) break;
            swap(pos, left);
            pos = left;
        } while (left < middle);
    }

    private void swap(int i, int j) {
        Node temp = nodes[i];
        nodes[i] = nodes[j];
        nodes[j] = temp;
    }

    private boolean nodeLess(HuffmanTree.Node n1, HuffmanTree.Node n2) {
        boolean result = true;
        if (n1.weight <= n2.weight) {
            if (n1.weight >= n2.weight) {
                result = n1.num < n2.num;
            } else {
                result = false;
            }
        }
        return result;
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
            return r != 0 ? r : Integer.compare(o.num, num);
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


}
