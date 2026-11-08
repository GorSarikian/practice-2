package nugetgraph;

import java.nio.file.Path;

public final class Main {
    private static final int STAGE_ARGUMENTS = 2;
    private Main() { }

    public static void main(String[] args) {
        try { execute(args); }
        catch (Exception exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void execute(String[] args) throws Exception {
        if (args.length == 1) { args = new String[] { "stage5", args[0] }; }
        if (args.length != STAGE_ARGUMENTS || !java.util.Set.of("stage1", "stage2", "stage3", "stage4", "stage5")
                .contains(args[0])) {
            throw new IllegalArgumentException("Usage: run.bat [stage1|stage2|stage3|stage4|stage5] <config.toml>");
        }
        Config config = Config.load(Path.of(args[1]));
        if (args[0].equals("stage1")) { printConfig(config); return; }
        Repository repository = config.testMode() ? new TestRepository(Path.of(config.repository()))
                : new NugetRepository(config);
        PackageRef root = root(repository, config);
        if (args[0].equals("stage4")) {
            Graph reversed = ReverseDependencies.find(repository, root, config);
            System.out.println("Reverse edges: dependency -> dependent");
            System.out.print(GraphText.adjacency(reversed));
            return;
        }
        if (!args[0].equals("stage2")) { graphStage(args[0], repository, root, config); return; }
        System.out.println(root.label());
        for (PackageRef dependency : repository.dependencies(root)) { System.out.println(dependency.label()); }
    }

    private static void graphStage(String stage, Repository repository, PackageRef root, Config config)
            throws Exception {
        Graph graph = new GraphBuilder(repository, config.maxDepth(), config.filter()).build(root);
        if (stage.equals("stage3")) { System.out.print(GraphText.adjacency(graph)); return; }
        if (config.asciiTree()) { System.out.print(AsciiTree.render(graph, config.maxDepth())); }
        String executable = System.getenv().getOrDefault("D2_COMMAND", "d2");
        Path source = D2Exporter.export(graph, config.output(), executable);
        System.out.println("D2: " + source);
        System.out.println("PNG: " + config.output());
    }

    private static PackageRef root(Repository repository, Config config) throws java.io.IOException {
        if (config.testMode()) { return repository.resolve(config.packageName(), ""); }
        String version = config.option("package_version", "latest");
        String range = version.equals("latest") ? version : "[" + new Version(version) + "]";
        return repository.resolve(config.packageName(), range);
    }

    private static void printConfig(Config config) {
        System.out.println("package=" + config.packageName());
        System.out.println("repository=" + config.repository());
        System.out.println("test_mode=" + config.testMode());
        System.out.println("output=" + config.output());
        System.out.println("ascii_tree=" + config.asciiTree());
        System.out.println("max_depth=" + config.maxDepth());
        System.out.println("filter=" + config.filter());
        config.options().keySet().stream().sorted().filter(SetHolder.OPTIONAL::contains)
                .forEach(key -> System.out.println(key + "=" + config.options().get(key)));
    }

    private static final class SetHolder {
        private static final java.util.Set<String> OPTIONAL =
                java.util.Set.of("package_version", "framework", "reverse_package");
    }
}
