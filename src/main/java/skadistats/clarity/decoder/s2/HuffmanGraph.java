package skadistats.clarity.decoder.s2;

public class HuffmanGraph {

    private final FieldPathDecoder.Node root;
    private final StringBuilder g;

    public HuffmanGraph(FieldPathDecoder.Node root) {
        this.root = root;
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

    public void genNodesRecursive(FieldPathDecoder.Node node, String path) {
        if (node instanceof FieldPathDecoder.InternalNode) {
            FieldPathDecoder.InternalNode internal = (FieldPathDecoder.InternalNode) node;
            g.append(String.format("%s [label=%s];\n", internal.getNum(), internal.getWeight()));
            genNodesRecursive(internal.getLeft(), path + "0");
            genNodesRecursive(internal.getRight(), path + "1");
        } else {
            FieldPathDecoder.LeafNode leaf = (FieldPathDecoder.LeafNode) node;
            g.append(String.format("%s [shape=record, label=\"{{%s|%s}|%s}\"];\n", leaf.getNum(), path, leaf.getWeight(), leaf.getOp().toString()));
        }
    }

    public void genEdgesRecursive(FieldPathDecoder.Node node) {
        if (node instanceof FieldPathDecoder.InternalNode) {
            FieldPathDecoder.InternalNode internal = (FieldPathDecoder.InternalNode) node;
            g.append(String.format("%s -> %s [label=0];\n", internal.getNum(), internal.getLeft().getNum()));
            genEdgesRecursive(internal.getLeft());
            g.append(String.format("%s -> %s [label=1];\n", internal.getNum(), internal.getRight().getNum()));
            genEdgesRecursive(internal.getRight());
        }
    }


}
