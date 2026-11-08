package nugetgraph;

import java.nio.file.Files;
import java.nio.file.Path;

/** Checks graph text, escaping, tree markers and renderer failure behavior. */
final class VisualizationTests {
    private VisualizationTests() { }

    static void run() throws Exception {
        TestRepository repository = new TestRepository(Path.of("examples/graph.txt"));
        Graph graph = new GraphBuilder(repository, 8, "").build(repository.resolve("A", ""));
        String tree = AsciiTree.render(graph, 8);
        Checks.equal(true, tree.contains("A [cycle]"));
        Checks.equal(true, tree.contains("D [shared]"));
        Checks.equal(true, tree.contains("|-- B"));
        Checks.equal(false, tree.contains("F"));
        String d2 = D2Exporter.text(graph);
        Checks.equal(true, d2.contains("n0: \"A\""));
        Checks.equal(true, d2.contains("n2 -> n0"));
        Checks.equal(d2, D2Exporter.text(graph));
        Graph boundary = new GraphBuilder(repository, 0, "").build(graph.root());
        Checks.equal(true, AsciiTree.render(boundary, 0).contains("[depth limit]"));
        Graph empty = new GraphBuilder(repository, 5, "A").build(graph.root());
        Checks.equal(true, D2Exporter.text(empty).contains("Root excluded"));
        failedRenderer(graph);
    }

    private static void failedRenderer(Graph graph) throws Exception {
        Path directory = Files.createTempDirectory("export-test-");
        Path output = directory.resolve("graph.png");
        try {
            Files.writeString(output, "existing output");
            Checks.fails(() -> D2Exporter.export(graph, output, "definitely-nonexistent-d2-command"));
            Checks.equal("existing output", Files.readString(output));
            String extension = System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
            String wrongRenderer = Path.of(System.getProperty("java.home"), "bin", "java" + extension).toString();
            Checks.fails(() -> D2Exporter.export(graph, output, wrongRenderer));
            Checks.equal("existing output", Files.readString(output));
            Checks.equal(true, Files.readString(directory.resolve("graph.d2")).contains("direction: right"));
            try (var files = Files.list(directory)) { Checks.equal(2L, files.count()); }
        } finally {
            Files.deleteIfExists(output);
            Files.deleteIfExists(directory.resolve("graph.d2"));
            Files.delete(directory);
        }
    }
}
