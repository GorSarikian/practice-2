package nugetgraph;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Recursive DFS with active-path cycle protection and remaining-depth memoization. */
public final class GraphBuilder {
    private final Repository repository;
    private final int maxDepth;
    private final String filter;
    private final Map<PackageRef, Integer> expandedBudget = new HashMap<>();
    private final Set<PackageRef> active = new HashSet<>();
    private Graph graph;

    /** Configures a bounded recursive traversal. */
    public GraphBuilder(Repository repository, int maxDepth, String filter) {
        if (maxDepth < 0 || maxDepth > Config.MAX_DEPTH) {
            throw new IllegalArgumentException("Depth outside 0.." + Config.MAX_DEPTH);
        }
        this.repository = repository;
        this.maxDepth = maxDepth;
        this.filter = filter.toLowerCase(Locale.ROOT);
    }

    /** Builds the reachable graph; a node encountered later at a smaller depth is expanded again. */
    public Graph build(PackageRef root) throws IOException {
        graph = new Graph(root);
        expandedBudget.clear();
        active.clear();
        if (!excluded(root)) { visit(root, maxDepth); }
        return graph;
    }

    private void visit(PackageRef node, int remaining) throws IOException {
        graph.addNode(node);
        if (active.contains(node)) { return; }
        if (expandedBudget.getOrDefault(node, -1) >= remaining) { return; }
        expandedBudget.put(node, remaining);
        if (remaining == 0) { graph.markTruncated(node); return; }
        graph.markExpanded(node);
        active.add(node);
        try {
            for (PackageRef child : repository.dependencies(node)) {
                if (excluded(child)) { continue; }
                graph.addEdge(node, child);
                visit(child, remaining - 1);
            }
        } finally { active.remove(node); }
    }

    private boolean excluded(PackageRef node) {
        return !filter.isEmpty() && node.name().toLowerCase(Locale.ROOT).contains(filter);
    }
}
