package nugetgraph;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** NuGet V3 service discovery, version selection and direct nuspec dependency reads. */
public final class NugetRepository implements Repository {
    private final HttpText http;
    private final URI base;
    private final Framework framework;
    private final String filter;
    private final Map<String, List<Version>> versions = new HashMap<>();
    private final Map<PackageRef, List<PackageRef>> dependencies = new HashMap<>();

    /** Uses JDK HTTP; no NuGet client or package manager is invoked. */
    public NugetRepository(Config config) throws IOException { this(config, HttpText.standard()); }

    /** Allows an in-memory transport for deterministic API and error tests. */
    public NugetRepository(Config config, HttpText http) throws IOException {
        this.http = http;
        framework = new Framework(config.option("framework", "net8.0"));
        filter = config.filter().toLowerCase(Locale.ROOT);
        base = discover(URI.create(config.repository()));
    }

    /** Uses latest stable for 'latest', otherwise the lowest satisfying dependency version. */
    @Override
    public PackageRef resolve(String name, String range) throws IOException {
        String id = name.toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_][a-z0-9_.-]*")) { throw new IOException("Invalid NuGet id: " + name); }
        List<Version> available = versions(id);
        boolean latest = range.equals("latest");
        VersionRange constraint = new VersionRange(latest ? "" : range);
        Version chosen = null;
        for (Version candidate : available) {
            if (constraint.contains(candidate)) {
                if (chosen == null || (latest && candidate.compareTo(chosen) > 0)
                        || (!latest && candidate.compareTo(chosen) < 0)) { chosen = candidate; }
            }
        }
        if (chosen == null) { throw new IOException("No version of " + name + " satisfies " + range); }
        return new PackageRef(id, chosen.toString());
    }

    /** Resolves direct dependencies of the nearest compatible framework group, with caching. */
    @Override
    public List<PackageRef> dependencies(PackageRef node) throws IOException {
        if (dependencies.containsKey(node)) { return dependencies.get(node); }
        URI uri = base.resolve(node.name() + "/" + node.version() + "/" + node.name() + ".nuspec");
        List<PackageRef> result = new ArrayList<>();
        for (Nuspec.Requirement requirement : Nuspec.read(http.get(uri), framework)) {
            if (!filter.isEmpty() && requirement.name().toLowerCase(Locale.ROOT).contains(filter)) { continue; }
            result.add(resolve(requirement.name(), requirement.range()));
        }
        List<PackageRef> immutable = List.copyOf(result);
        dependencies.put(node, immutable);
        return immutable;
    }

    private List<Version> versions(String id) throws IOException {
        if (!versions.containsKey(id)) {
            Map<?, ?> response = object(http.get(base.resolve(id + "/index.json")));
            Object raw = response.get("versions");
            if (!(raw instanceof List<?> list)) { throw new IOException("NuGet version list is missing"); }
            List<Version> parsed = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof String value)) { throw new IOException("Invalid NuGet version entry"); }
                parsed.add(new Version(value));
            }
            versions.put(id, List.copyOf(parsed));
        }
        return versions.get(id);
    }

    private URI discover(URI index) throws IOException {
        Object resources = object(http.get(index)).get("resources");
        if (!(resources instanceof List<?> list)) { throw new IOException("NuGet service index has no resources"); }
        for (Object entry : list) {
            if (entry instanceof Map<?, ?> map && "PackageBaseAddress/3.0.0".equals(map.get("@type"))) {
                if (!(map.get("@id") instanceof String address)) { throw new IOException("Invalid base address"); }
                URI uri = index.resolve(address.endsWith("/") ? address : address + "/");
                if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
                    throw new IOException("Package base address must be HTTP(S)");
                }
                return uri;
            }
        }
        throw new IOException("PackageBaseAddress/3.0.0 is missing in NuGet service index");
    }

    private static Map<?, ?> object(String text) throws IOException {
        Object value = Json.parse(text);
        if (!(value instanceof Map<?, ?> map)) { throw new IOException("Expected a JSON object"); }
        return map;
    }
}
