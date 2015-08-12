package skadistats.clarity.decoder.s2;

public class HuffmanGraph {

    private final HuffmanTree.Node root;
    private final StringBuilder g;

    public HuffmanGraph(HuffmanTree tree) {
        this.root = tree.root;
        this.g = new StringBuilder();
    }

    public String generate() {
        g.append("digraph G {\n");
        g.append("graph [ranksep=0];\n");
        genNodesRecursive(root, "");
        genEdgesRecursive(root);
        g.append("}");
        return g.toString();
    }

    public void genNodesRecursive(HuffmanTree.Node node, String path) {
        if (node instanceof HuffmanTree.InternalNode) {
            g.append(String.format("%s [label=%s];\n", node.num, node.weight));
            if (node.left != null)
                genNodesRecursive(node.left, path + "0");
            if (node.right != null)
                genNodesRecursive(node.right, path + "1");
        } else {
            g.append(String.format("%s [shape=record, label=\"{{%s|%s}|%s}\"];\n", node.num, path, node.weight, node.op));
        }
    }

    public void genEdgesRecursive(HuffmanTree.Node node) {
        if (node instanceof HuffmanTree.InternalNode) {
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
