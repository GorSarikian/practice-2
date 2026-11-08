package nugetgraph;

/** NuGet interval or minimum-version constraint; floating nuspec ranges are rejected. */
public final class VersionRange {
    private static final int INTERVAL_BOUNDS = 2;
    private Version lower;
    private Version upper;
    private boolean includeLower;
    private boolean includeUpper;
    private final boolean allowPrerelease;

    /** Parses bare minimum, [exact], [min,max), (,max], and unbounded constraints. */
    public VersionRange(String raw) {
        String value = raw.trim();
        allowPrerelease = value.contains("-");
        if (value.isEmpty() || value.equals("(,)")) {
            return;
        } else if (!value.startsWith("[") && !value.startsWith("(")) {
            lower = new Version(value); upper = null; includeLower = true; includeUpper = false;
        } else { interval(value); }
        if (lower != null && upper != null && lower.compareTo(upper) > 0) {
            throw new IllegalArgumentException("Reversed version bounds: " + raw);
        }
    }

    private void interval(String value) {
        if (!value.endsWith("]") && !value.endsWith(")")) {
            throw new IllegalArgumentException("Invalid version range: " + value);
        }
        includeLower = value.startsWith("[");
        includeUpper = value.endsWith("]");
        String[] bounds = value.substring(1, value.length() - 1).split(",", -1);
        lower = bound(bounds[0]);
        if (bounds.length == 1) { exact(); return; }
        if (bounds.length != INTERVAL_BOUNDS) { throw new IllegalArgumentException("Invalid version range: " + value); }
        upper = bound(bounds[1]);
    }

    private static Version bound(String text) { return text.isBlank() ? null : new Version(text.trim()); }

    private void exact() {
        if (!includeLower || !includeUpper || lower == null) {
            throw new IllegalArgumentException("Exact version must use [version]");
        }
        upper = lower;
    }

    /** Whether a concrete version satisfies both bounds and prerelease policy. */
    public boolean contains(Version version) {
        if (version.prerelease() && !allowPrerelease) { return false; }
        return aboveLower(version) && belowUpper(version);
    }

    private boolean aboveLower(Version version) {
        if (lower == null) { return true; }
        int order = version.compareTo(lower);
        return order > 0 || (order == 0 && includeLower);
    }

    private boolean belowUpper(Version version) {
        if (upper == null) { return true; }
        int order = version.compareTo(upper);
        return order < 0 || (order == 0 && includeUpper);
    }
}
