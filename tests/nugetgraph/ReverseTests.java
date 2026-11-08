package nugetgraph;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/** Checks reverse traversal across cycles, depth limits and disconnected components. */
final class ReverseTests {
    private ReverseTests() { }

    static void run() throws Exception {
        Config config = Config.load(Path.of("examples/reverse.toml"));
        TestRepository repository = new TestRepository(Path.of(config.repository()));
        PackageRef root = repository.resolve("A", "");
        Graph graph = ReverseDependencies.find(repository, root, config);
        Checks.equal(Set.of("A", "B", "C", "D", "E", "F"), names(graph));
        Checks.equal(Set.of("C", "F"), graph.children(repository.resolve("E", "")).stream()
                .map(PackageRef::name).collect(Collectors.toSet()));
        Config shallow = changed(config, "E", 1, "");
        Checks.equal(Set.of("E", "C", "F"), names(ReverseDependencies.find(repository, root, shallow)));
        Checks.equal(Set.of("G"), names(ReverseDependencies.find(repository, root, changed(config, "G", 8, ""))));
        Checks.equal(Set.of("E", "F"), names(ReverseDependencies.find(repository, root, changed(config, "E", 8, "C"))));
        Checks.fails(() -> ReverseDependencies.find(repository, root, changed(config, "Z", 8, "")));
        Checks.equal(Set.of("A", "B", "C", "D"), names(ReverseDependencies.find(repository, root,
                changed(config, "A", 8, ""))));
    }

    private static Config changed(Config source, String target, int depth, String filter) {
        var options = new HashMap<>(source.options());
        options.put("reverse_package", target);
        return new Config(source.packageName(), source.repository(), true, source.output(), true,
                depth, filter, options);
    }

    private static Set<String> names(Graph graph) {
        return graph.nodes().stream().map(PackageRef::name).collect(Collectors.toSet());
    }
}
