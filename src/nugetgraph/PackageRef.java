package nugetgraph;

public record PackageRef(String name, String version) {
    public String label() { return version.isEmpty() ? name : name + "@" + version; }
}
