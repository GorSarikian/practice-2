package nugetgraph;

/** A graph node identifies one concrete package version; test nodes have no version. */
public record PackageRef(String name, String version) {
    /** Returns a readable node label. */
    public String label() { return version.isEmpty() ? name : name + "@" + version; }
}
