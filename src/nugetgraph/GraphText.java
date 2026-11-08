package nugetgraph;

/** Plain adjacency output for the graph-building stage. */
public final class GraphText {
    private GraphText() { }

    /** Describes every discovered node and marks unexplored depth boundaries. */
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
