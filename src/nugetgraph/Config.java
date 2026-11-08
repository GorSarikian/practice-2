package nugetgraph;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Validated user parameters; relative paths are resolved against the TOML file. */
public record Config(String packageName, String repository, boolean testMode, Path output,
                     boolean asciiTree, int maxDepth, String filter, Map<String, Object> options) {
    public static final int MAX_DEPTH = 128;
    private static final Set<String> KEYS = Set.of("package", "repository", "test_mode", "output",
            "ascii_tree", "max_depth", "filter", "package_version", "framework", "reverse_package");

    /** Loads and validates required parameters, rejecting unknown keys and type mistakes. */
    public static Config load(Path file) throws IOException {
        Map<String, Object> values = Toml.read(file);
        for (String key : values.keySet()) {
            if (!KEYS.contains(key)) { throw new IllegalArgumentException("Unknown parameter: " + key); }
        }
        String name = required(values, "package", String.class);
        boolean test = required(values, "test_mode", Boolean.class);
        validateName(name, test);
        Path parent = file.toAbsolutePath().getParent();
        String source = validateRepository(required(values, "repository", String.class), test, parent);
        Path output = validateOutput(required(values, "output", String.class), parent);
        boolean ascii = required(values, "ascii_tree", Boolean.class);
        int depth = required(values, "max_depth", Integer.class);
        if (depth < 0 || depth > MAX_DEPTH) {
            throw new IllegalArgumentException("max_depth must be in 0.." + MAX_DEPTH);
        }
        String filter = required(values, "filter", String.class);
        validateOptions(values, test);
        return new Config(name, source, test, output, ascii, depth, filter, Map.copyOf(values));
    }

    /** Returns an optional string parameter, or the documented default. */
    public String option(String key, String fallback) {
        return (String) options.getOrDefault(key, fallback);
    }

    private static void validateOptions(Map<String, Object> values, boolean test) {
        for (String key : Set.of("package_version", "framework", "reverse_package")) {
            if (values.containsKey(key)) {
                String value = required(values, key, String.class);
                if (value.isBlank()) { throw new IllegalArgumentException(key + " must not be blank"); }
            }
        }
        if (values.containsKey("reverse_package")) { validateName((String) values.get("reverse_package"), test); }
    }

    private static <T> T required(Map<String, Object> values, String key, Class<T> type) {
        if (!type.isInstance(values.get(key))) {
            throw new IllegalArgumentException(key + " is required and must be " + type.getSimpleName());
        }
        return type.cast(values.get(key));
    }

    private static void validateName(String name, boolean test) {
        String pattern = test ? "[A-Z]+" : "[A-Za-z0-9_][A-Za-z0-9_.-]*";
        if (!name.matches(pattern)) { throw new IllegalArgumentException("Invalid package name: " + name); }
    }

    private static String validateRepository(String value, boolean test, Path parent) {
        if (value.isBlank()) { throw new IllegalArgumentException("repository must not be blank"); }
        if (test) {
            Path path = parent.resolve(value).normalize();
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw new IllegalArgumentException("repository must be a readable file: " + path);
            }
            return path.toString();
        }
        URI uri = URI.create(value);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null) {
            throw new IllegalArgumentException("repository must be an HTTP(S) NuGet V3 index URL");
        }
        return value;
    }

    private static Path validateOutput(String value, Path parent) {
        if (value.isBlank() || !value.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            throw new IllegalArgumentException("output must be a nonempty PNG file path");
        }
        Path path = parent.resolve(value).normalize();
        if (Files.isDirectory(path)) { throw new IllegalArgumentException("output points to a directory"); }
        ensureWritable(path);
        return path;
    }

    private static void ensureWritable(Path path) {
        Path ancestor = path.getParent();
        while (ancestor != null && !Files.exists(ancestor)) { ancestor = ancestor.getParent(); }
        if (ancestor == null || !Files.isDirectory(ancestor) || !Files.isWritable(ancestor)) {
            throw new IllegalArgumentException("output parent must be writable");
        }
        if (Files.exists(path) && !Files.isWritable(path)) {
            throw new IllegalArgumentException("output file must be writable");
        }
    }
}
