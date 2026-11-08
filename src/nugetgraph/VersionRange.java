package nugetgraph;

/** NuGet interval or minimum-version constraint; floating nuspec ranges are rejected. */
public final class VersionRange {
    private final Version lower;
    private final Version upper;
    private final boolean includeLower;
    private final boolean includeUpper;
    private final boolean allowPrerelease;

    /** Parses bare minimum, [exact], [min,max), (,max], and unbounded constraints. */
    public VersionRange(String raw) {
        String value = raw.trim();
        allowPrerelease = value.contains("-");
        if (value.isEmpty() || value.equals("(,)")) {
            lower = null; upper = null; includeLower = false; includeUpper = false;
        } else if (!value.startsWith("[") && !value.startsWith("(")) {
            lower = new Version(value); upper = null; includeLower = true; includeUpper = false;
        } else {
            if (!value.endsWith("]") && !value.endsWith(")")) {
                throw new IllegalArgumentException("Invalid version range: " + raw);
            }
            includeLower = value.startsWith("[");
            includeUpper = value.endsWith("]");
            String[] bounds = value.substring(1, value.length() - 1).split(",", -1);
            lower = bounds[0].isBlank() ? null : new Version(bounds[0].trim());
            if (bounds.length == 1) {
                if (!includeLower || !includeUpper || lower == null) {
                    throw new IllegalArgumentException("Exact version must use [version]");
                }
                upper = lower;
            } else {
                if (bounds.length != 2) { throw new IllegalArgumentException("Invalid version range: " + raw); }
                upper = bounds[1].isBlank() ? null : new Version(bounds[1].trim());
            }
        }
        if (lower != null && upper != null && lower.compareTo(upper) > 0) {
            throw new IllegalArgumentException("Reversed version bounds: " + raw);
        }
    }

    /** Whether a concrete version satisfies both bounds and prerelease policy. */
    public boolean contains(Version version) {
        if (version.prerelease() && !allowPrerelease) { return false; }
        if (lower != null) {
            int order = version.compareTo(lower);
            if (order < 0 || (order == 0 && !includeLower)) { return false; }
        }
        if (upper != null) {
            int order = version.compareTo(upper);
            if (order > 0 || (order == 0 && !includeUpper)) { return false; }
        }
        return true;
    }
}
