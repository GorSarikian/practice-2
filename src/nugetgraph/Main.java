package nugetgraph;

import java.nio.file.Path;

/** Command-line entry point and stage demonstrations. */
public final class Main {
    private Main() { }

    /** Runs a stage; failures use stderr and a nonzero process exit status. */
    public static void main(String[] args) {
        try { execute(args); }
        catch (Exception exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void execute(String[] args) throws Exception {
        if (args.length != 2 || !java.util.Set.of("stage1", "stage2").contains(args[0])) {
            throw new IllegalArgumentException("Usage: run.bat stage1|stage2 <config.toml>");
        }
        Config config = Config.load(Path.of(args[1]));
        if (args[0].equals("stage1")) { printConfig(config); return; }
        if (config.testMode()) { throw new IllegalArgumentException("Test repository is introduced at stage3"); }
        Repository repository = new NugetRepository(config);
        PackageRef root = root(repository, config);
        System.out.println(root.label());
        for (PackageRef dependency : repository.dependencies(root)) { System.out.println(dependency.label()); }
    }

    private static PackageRef root(Repository repository, Config config) throws java.io.IOException {
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
        config.options().keySet().stream().sorted().filter(key ->
                SetHolder.OPTIONAL.contains(key)).forEach(key -> System.out.println(key + "=" + config.options().get(key)));
    }

    private static final class SetHolder {
        private static final java.util.Set<String> OPTIONAL =
                java.util.Set.of("package_version", "framework", "reverse_package");
    }
}
