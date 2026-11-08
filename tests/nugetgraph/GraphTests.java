package nugetgraph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

final class GraphTests {
    private GraphTests() { }

    static void run() throws Exception {
        TestRepository repository = new TestRepository(Path.of("examples/graph.txt"));
        PackageRef root = repository.resolve("A", "");
        Graph graph = new GraphBuilder(repository, 8, "").build(root);
        Checks.equal(Set.of("A", "B", "C", "D", "E"), names(graph));
        Checks.equal(List.of(root), graph.children(repository.resolve("D", "")));
        Graph zero = new GraphBuilder(repository, 0, "").build(root);
        Checks.equal(List.of(root), zero.nodes());
        Checks.equal(true, zero.truncated(root));
        Graph one = new GraphBuilder(repository, 1, "").build(root);
        Checks.equal(Set.of("A", "B", "C"), names(one));
        Checks.equal(true, one.truncated(repository.resolve("B", "")));
        Graph filtered = new GraphBuilder(repository, 8, "d").build(root);
        Checks.equal(Set.of("A", "B", "C", "E"), names(filtered));
        Checks.equal(List.of(), new GraphBuilder(repository, 8, "a").build(root).nodes());
        Checks.fails(() -> repository.resolve("Z", ""));
        Checks.fails(() -> new GraphBuilder(repository, -1, ""));
        shorterPath();
        invalidFiles();
    }

    private static void shorterPath() throws Exception {
        Path file = Files.createTempFile("graph-", ".txt");
        try {
            Files.writeString(file, "A: B D\nB: C\nC: D\nD: E\nE: F\nF:\n");
            TestRepository repository = new TestRepository(file);
            Graph graph = new GraphBuilder(repository, 3, "").build(repository.resolve("A", ""));
            Checks.equal(Set.of("A", "B", "C", "D", "E", "F"), names(graph));
            Checks.equal(false, graph.truncated(repository.resolve("D", "")));
            Checks.equal(true, graph.truncated(repository.resolve("F", "")));
            Files.writeString(file, "A: A\n");
            repository = new TestRepository(file);
            Graph self = new GraphBuilder(repository, 5, "").build(repository.resolve("A", ""));
            Checks.equal(List.of(self.root()), self.children(self.root()));
        } finally { Files.delete(file); }
    }

    private static void invalidFiles() throws Exception {
        Path file = Files.createTempFile("graph-", ".txt");
        try {
            for (String source : List.of("A: B\n", "A:\nA:\n", "a:\n", "A -> B\n", "# empty\n")) {
                Files.writeString(file, source);
                Checks.fails(() -> new TestRepository(file));
            }
        } finally { Files.delete(file); }
    }

    private static Set<String> names(Graph graph) {
        return graph.nodes().stream().map(PackageRef::name).collect(java.util.stream.Collectors.toSet());
    }
}
