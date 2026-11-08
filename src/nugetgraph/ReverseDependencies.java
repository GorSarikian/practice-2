package nugetgraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ReverseDependencies {
    private ReverseDependencies() { }

    public static Graph find(Repository repository, PackageRef root, Config config) throws IOException {
        Graph source = sourceGraph(repository, root, config);
        String name = config.option("reverse_package", config.packageName());
        List<PackageRef> targets = source.nodes().stream()
                .filter(node -> node.name().equalsIgnoreCase(name)).toList();
        if (targets.isEmpty()) { throw new IOException("Reverse target is absent from analyzed scope: " + name); }
        Graph reversed = reverse(source);
        Graph result = new Graph(targets.get(0));
        Repository view = new GraphRepository(reversed);
        for (PackageRef target : targets) {
            Graph found = new GraphBuilder(view, config.maxDepth(), config.filter()).build(target);
            merge(result, found);
        }
        return result;
    }

    private static Graph sourceGraph(Repository repository, PackageRef root, Config config) throws IOException {
        if (!(repository instanceof TestRepository test)) {
            return new GraphBuilder(repository, config.maxDepth(), config.filter()).build(root);
        }
        Graph result = new Graph(root);
        String filter = config.filter().toLowerCase(java.util.Locale.ROOT);
        for (PackageRef node : test.packages()) {
            if (!filter.isEmpty() && node.name().toLowerCase(java.util.Locale.ROOT).contains(filter)) { continue; }
            result.addNode(node);
            for (PackageRef child : test.dependencies(node)) {
                if (filter.isEmpty() || !child.name().toLowerCase(java.util.Locale.ROOT).contains(filter)) {
                    result.addEdge(node, child);
                }
            }
        }
        return result;
    }

    private static Graph reverse(Graph source) {
        Graph result = new Graph(source.root());
        for (PackageRef node : source.nodes()) {
            result.addNode(node);
            for (PackageRef child : source.children(node)) { result.addEdge(child, node); }
        }
        return result;
    }

    private static void merge(Graph result, Graph found) {
        for (PackageRef node : found.nodes()) {
            result.addNode(node);
            for (PackageRef child : found.children(node)) { result.addEdge(node, child); }
            if (found.truncated(node) && result.children(node).isEmpty()) { result.markTruncated(node); }
            if (!found.truncated(node)) { result.markExpanded(node); }
        }
    }

    private record GraphRepository(Graph graph) implements Repository {
        @Override
        public PackageRef resolve(String name, String range) throws IOException {
            return graph.nodes().stream().filter(node -> node.name().equalsIgnoreCase(name)).findFirst()
                    .orElseThrow(() -> new IOException("Unknown graph node: " + name));
        }

        @Override
        public List<PackageRef> dependencies(PackageRef node) { return new ArrayList<>(graph.children(node)); }
    }
}
