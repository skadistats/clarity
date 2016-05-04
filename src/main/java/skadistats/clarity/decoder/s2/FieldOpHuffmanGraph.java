package skadistats.clarity.decoder.s2;

public class FieldOpHuffmanGraph {

    private final StringBuilder g;

    public FieldOpHuffmanGraph(FieldOpHuffmanTree tree) {
        this.g = new StringBuilder();
    }

    public String generate() {
        g.append("digraph G {\n");
        g.append("graph [ranksep=0];\n");
        genNodesRecursive(FieldOpHuffmanTree.root, "");
        genEdgesRecursive(FieldOpHuffmanTree.root);
        g.append("}");
        return g.toString();
    }

    public void genNodesRecursive(FieldOpHuffmanTree.Node node, String path) {
        if (node instanceof FieldOpHuffmanTree.InternalNode) {
            g.append(String.format("%s [label=%s];\n", node.num, node.weight));
            if (node.left != null)
                genNodesRecursive(node.left, path + "0");
            if (node.right != null)
                genNodesRecursive(node.right, path + "1");
        } else {
            g.append(String.format("%s [shape=record, label=\"{{%s|%s}|%s}\"];\n", node.num, path, node.weight, node.op));
        }
    }

    public void genEdgesRecursive(FieldOpHuffmanTree.Node node) {
        if (node instanceof FieldOpHuffmanTree.InternalNode) {
            if (node.left != null) {
                g.append(String.format("%s -> %s [label=0];\n", node.num, node.left.num));
                genEdgesRecursive(node.left);
            }
            if (node.right != null) {
                g.append(String.format("%s -> %s [label=1];\n", node.num, node.right.num));
                genEdgesRecursive(node.right);
            }
        }
    }

}
