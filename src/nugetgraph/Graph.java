package nugetgraph;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Directed package-version graph in deterministic discovery order. */
public final class Graph {
    private final PackageRef root;
    private final Map<PackageRef, Set<PackageRef>> edges = new LinkedHashMap<>();
    private final Set<PackageRef> truncated = new LinkedHashSet<>();

    /** Creates an initially empty graph for the selected root. */
    public Graph(PackageRef root) { this.root = root; }

    /** Returns the selected root, which may be excluded by a filter. */
    public PackageRef root() { return root; }

    /** Returns discovered vertices in insertion order. */
    public List<PackageRef> nodes() { return List.copyOf(edges.keySet()); }

    /** Returns outgoing dependencies, or an empty list for an absent node. */
    public List<PackageRef> children(PackageRef node) {
        return List.copyOf(edges.getOrDefault(node, Set.of()));
    }

    /** Whether outgoing dependencies were not examined because of the depth limit. */
    public boolean truncated(PackageRef node) { return truncated.contains(node); }

    void addNode(PackageRef node) { edges.computeIfAbsent(node, ignored -> new LinkedHashSet<>()); }

    void addEdge(PackageRef parent, PackageRef child) {
        addNode(parent); addNode(child); edges.get(parent).add(child);
    }

    void markTruncated(PackageRef node) { truncated.add(node); }

    void markExpanded(PackageRef node) { truncated.remove(node); }
}
