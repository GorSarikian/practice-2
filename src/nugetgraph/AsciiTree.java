package nugetgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** ASCII dependency tree with explicit cycle, shared-node and depth markers. */
public final class AsciiTree {
    private final StringBuilder output = new StringBuilder();
    private final Set<PackageRef> active = new HashSet<>();
    private final Map<PackageRef, Integer> shownBudget = new HashMap<>();
    private final Graph graph;

    private AsciiTree(Graph graph) { this.graph = graph; }

    /** Formats an actual ASCII-only tree; package names may contain Unicode in future repositories. */
    public static String render(Graph graph, int maxDepth) {
        if (graph.nodes().isEmpty()) { return "Root excluded by filter\n"; }
        AsciiTree tree = new AsciiTree(graph);
        tree.line(graph.root(), "", "", maxDepth);
        return tree.output.toString();
    }

    private void line(PackageRef node, String prefix, String branch, int remaining) {
        output.append(prefix).append(branch).append(node.label());
        if (active.contains(node)) { output.append(" [cycle]\n"); return; }
        if (shownBudget.getOrDefault(node, -1) >= remaining) { output.append(" [shared]\n"); return; }
        shownBudget.put(node, remaining);
        if (remaining == 0 || graph.truncated(node)) { output.append(" [depth limit]\n"); return; }
        output.append('\n');
        active.add(node);
        var children = graph.children(node);
        String nextPrefix = prefix + (branch.isEmpty() ? "" : branch.equals("\\-- ") ? "    " : "|   ");
        for (int i = 0; i < children.size(); i++) {
            String nextBranch = i == children.size() - 1 ? "\\-- " : "|-- ";
            line(children.get(i), nextPrefix, nextBranch, remaining - 1);
        }
        active.remove(node);
    }
}
