package nugetgraph;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** NuGet numeric version with optional prerelease identifiers and build metadata. */
public final class Version implements Comparable<Version> {
    private static final int COMPONENTS = 4;
    private final int[] numbers = new int[COMPONENTS];
    private final List<String> prerelease;
    private final String normalized;

    /** Parses and normalizes 1-4 numeric components and an optional SemVer suffix. */
    public Version(String raw) {
        String value = raw.toLowerCase(Locale.ROOT).split("\\+", 2)[0];
        String[] parts = value.split("-", 2);
        if (!parts[0].matches("[0-9]+(\\.[0-9]+){0,3}")) {
            throw new IllegalArgumentException("Invalid NuGet version: " + raw);
        }
        String[] digits = parts[0].split("\\.");
        for (int i = 0; i < digits.length; i++) { numbers[i] = Integer.parseInt(digits[i]); }
        prerelease = parts.length == 1 ? List.of() : Arrays.asList(parts[1].split("\\.", -1));
        for (String identifier : prerelease) {
            if (!identifier.matches("[a-z0-9-]+")) {
                throw new IllegalArgumentException("Invalid prerelease version: " + raw);
            }
        }
        String numeric = numbers[0] + "." + numbers[1] + "." + numbers[2];
        if (numbers[3] != 0) { numeric += "." + numbers[3]; }
        normalized = numeric + (prerelease.isEmpty() ? "" : "-" + String.join(".", prerelease));
    }

    /** Whether this version contains a prerelease suffix. */
    public boolean prerelease() { return !prerelease.isEmpty(); }

    /** Returns NuGet's normalized lowercase version (metadata omitted). */
    @Override
    public String toString() { return normalized; }

    /** Compares numeric components, then SemVer prerelease identifiers. */
    @Override
    public int compareTo(Version other) {
        for (int i = 0; i < COMPONENTS; i++) {
            int result = Integer.compare(numbers[i], other.numbers[i]);
            if (result != 0) { return result; }
        }
        if (prerelease.isEmpty() || other.prerelease.isEmpty()) {
            return Boolean.compare(prerelease.isEmpty(), other.prerelease.isEmpty());
        }
        int common = Math.min(prerelease.size(), other.prerelease.size());
        for (int i = 0; i < common; i++) {
            int result = compareIdentifier(prerelease.get(i), other.prerelease.get(i));
            if (result != 0) { return result; }
        }
        return Integer.compare(prerelease.size(), other.prerelease.size());
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = left.matches("[0-9]+");
        boolean rightNumeric = right.matches("[0-9]+");
        if (leftNumeric && rightNumeric) { return new BigInteger(left).compareTo(new BigInteger(right)); }
        if (leftNumeric != rightNumeric) { return leftNumeric ? -1 : 1; }
        return left.compareTo(right);
    }
}
