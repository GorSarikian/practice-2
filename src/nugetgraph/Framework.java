package nugetgraph;

import java.util.Locale;

/** Limited, explicit framework compatibility for modern .NET and .NET Standard. */
public final class Framework {
    private static final int NET_STANDARD_BASE = 1000;
    private static final int NET_CORE_BASE = 2000;
    private static final int NET_BASE = 3000;
    private static final int MODERN_NET_MIN = 500;
    private static final int STANDARD_20 = 200;
    private static final int STANDARD_21 = 201;
    private static final int CORE_31 = 301;
    private final String target;

    /** Accepts net5.0+ or netstandard2.0/2.1; platform-specific TFMs are not guessed. */
    public Framework(String target) {
        this.target = normalize(target);
        if (this.target.equals("netstandard2.0") || this.target.equals("netstandard2.1")) { return; }
        if (!this.target.matches("net[0-9]+\\.[0-9]+") || level(this.target) < MODERN_NET_MIN) {
            throw new IllegalArgumentException("Supported frameworks: net5.0+ and netstandard2.0/2.1");
        }
    }

    /** Returns a compatibility score, or -1 for an incompatible dependency group. */
    public int score(String raw) {
        String group = normalize(raw);
        if (group.isEmpty()) { return 0; }
        if (group.startsWith("netstandard")) {
            int maximum = target.startsWith("netstandard") ? level(target) : STANDARD_21;
            return level(group) <= maximum ? NET_STANDARD_BASE + level(group) : -1;
        }
        if (target.startsWith("netstandard")) { return -1; }
        return modernScore(group);
    }

    private int modernScore(String group) {
        if (group.matches("net[0-9]+\\.[0-9]+") && level(group) >= MODERN_NET_MIN) {
            return level(group) <= level(target) ? NET_BASE + level(group) : -1;
        }
        if (group.matches("netcoreapp[0-9]+\\.[0-9]+") && level(group) <= CORE_31) {
            return NET_CORE_BASE + level(group);
        }
        return -1;
    }

    private static String normalize(String value) {
        String result = value.toLowerCase(Locale.ROOT).replace(" ", "");
        result = result.replace(".netstandard", "netstandard").replace(".netcoreapp", "netcoreapp");
        if (result.startsWith("netcoreapp") && level(result) >= MODERN_NET_MIN) {
            return "net" + result.substring("netcoreapp".length());
        }
        return result;
    }

    private static int level(String value) {
        String digits = value.replaceFirst("^[^0-9]*", "");
        if (!digits.matches("[0-9]+\\.[0-9]+")) { return Integer.MAX_VALUE; }
        String[] parts = digits.split("\\.");
        return Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
    }
}
