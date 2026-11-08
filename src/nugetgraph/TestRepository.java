package nugetgraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class TestRepository implements Repository {
    private static final int DECLARATION_FIELDS = 2;
    private final Map<PackageRef, List<PackageRef>> graph = new LinkedHashMap<>();

    public TestRepository(Path file) throws IOException {
        int number = 0;
        for (String raw : Files.readAllLines(file)) {
            number++;
            String line = raw.split("#", 2)[0].trim();
            if (line.isEmpty()) { continue; }
            String[] parts = line.split(":", -1);
            if (parts.length != DECLARATION_FIELDS) {
                throw new IOException("Graph line " + number + ": expected A: B C");
            }
            PackageRef node = node(parts[0].trim());
            List<PackageRef> children = new ArrayList<>();
            if (!parts[1].isBlank()) {
                for (String name : parts[1].trim().split("\\s+")) { children.add(node(name)); }
            }
            if (graph.putIfAbsent(node, List.copyOf(new LinkedHashSet<>(children))) != null) {
                throw new IOException("Duplicate graph node: " + node.name());
            }
        }
        validateReferences();
    }

    private void validateReferences() throws IOException {
        if (graph.isEmpty()) { throw new IOException("Test repository is empty"); }
        for (List<PackageRef> children : graph.values()) {
            for (PackageRef child : children) {
                if (!graph.containsKey(child)) { throw new IOException("Undeclared node: " + child.name()); }
            }
        }
    }

    @Override
    public PackageRef resolve(String name, String range) throws IOException {
        PackageRef node = node(name);
        if (!graph.containsKey(node)) { throw new IOException("Test package not found: " + name); }
        return node;
    }

    @Override
    public List<PackageRef> dependencies(PackageRef node) throws IOException {
        if (!graph.containsKey(node)) { throw new IOException("Test package not found: " + node.name()); }
        return graph.get(node);
    }

    public List<PackageRef> packages() { return List.copyOf(graph.keySet()); }

    private static PackageRef node(String name) throws IOException {
        if (!name.matches("[A-Z]+")) { throw new IOException("Test package names must contain only A-Z: " + name); }
        return new PackageRef(name, "");
    }
}
