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
        if (args.length != 2 || !args[0].equals("stage1")) {
            throw new IllegalArgumentException("Usage: run.bat stage1 <config.toml>");
        }
        Config config = Config.load(Path.of(args[1]));
        printConfig(config);
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
