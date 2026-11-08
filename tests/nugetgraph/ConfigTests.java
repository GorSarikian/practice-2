package nugetgraph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class ConfigTests {
    private ConfigTests() { }

    static void run() throws Exception {
        Config valid = Config.load(Path.of("examples/test.toml"));
        Checks.equal("A", valid.packageName());
        Checks.equal(true, valid.testMode());
        Checks.equal(5, valid.maxDepth());
        String source = Files.readString(Path.of("examples/test.toml"));
        var required = List.of("package", "repository", "test_mode", "output", "ascii_tree", "max_depth", "filter");
        for (String key : required) {
            invalid(source.replaceFirst("(?m)^" + key + " = .*", key + " = 1.5"));
            invalid(source.replaceFirst("(?m)^" + key + " = .*\\R?", ""));
        }
        invalid(source.replace("\"A\"", "\"a\""));
        invalid(source.replace("graph.txt", "missing.txt"));
        invalid(source.replace("test.png", "test.svg"));
        invalid(source.replace("max_depth = 5", "max_depth = -1"));
        invalid(source.replace("max_depth = 5", "max_depth = 129"));
        invalid(source + "package = \"B\"\n");
        invalid(source + "unknown = true\n");
        invalid(source.replace("test_mode = true", "test_mode = false"));
        lexical();
    }

    private static void invalid(String source) throws Exception {
        Path file = Files.createTempFile(Path.of("examples"), "invalid-", ".toml");
        try {
            Files.writeString(file, source);
            Checks.fails(() -> Config.load(file));
        } finally { Files.delete(file); }
    }

    private static void lexical() throws Exception {
        Path file = Files.createTempFile("toml-", ".toml");
        try {
            Files.writeString(file, "text = \"a#b\\n\\u0410\" # comment\nflag = true\nnumber = 1_000\n");
            var values = Toml.read(file);
            Checks.equal("a#b\nА", values.get("text"));
            Checks.equal(1000, values.get("number"));
            Files.writeString(file, "x = \"bad\\q\"\n");
            Checks.fails(() -> Toml.read(file));
        } finally { Files.delete(file); }
    }
}
