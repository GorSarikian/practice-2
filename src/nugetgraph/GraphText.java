package nugetgraph;

public final class GraphText {
    private GraphText() { }

    public static String adjacency(Graph graph) {
        if (graph.nodes().isEmpty()) { return "Root excluded by filter\n"; }
        StringBuilder output = new StringBuilder();
        for (PackageRef node : graph.nodes()) {
            output.append(node.label()).append(": ");
            output.append(String.join(", ", graph.children(node).stream().map(PackageRef::label).toList()));
            if (graph.truncated(node)) { output.append(" [depth limit]"); }
            output.append('\n');
        }
        return output.toString();
    }
}
