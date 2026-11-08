package nugetgraph;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Graph {
    private final PackageRef root;
    private final Map<PackageRef, Set<PackageRef>> edges = new LinkedHashMap<>();
    private final Set<PackageRef> truncated = new LinkedHashSet<>();

    public Graph(PackageRef root) { this.root = root; }

    public PackageRef root() { return root; }

    public List<PackageRef> nodes() { return List.copyOf(edges.keySet()); }

    public List<PackageRef> children(PackageRef node) {
        return List.copyOf(edges.getOrDefault(node, Set.of()));
    }

    public boolean truncated(PackageRef node) { return truncated.contains(node); }

    void addNode(PackageRef node) { edges.computeIfAbsent(node, ignored -> new LinkedHashSet<>()); }

    void addEdge(PackageRef parent, PackageRef child) {
        addNode(parent); addNode(child); edges.get(parent).add(child);
    }

    void markTruncated(PackageRef node) { truncated.add(node); }

    void markExpanded(PackageRef node) { truncated.remove(node); }
}
