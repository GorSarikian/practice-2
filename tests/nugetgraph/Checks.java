package nugetgraph;

/** Small dependency-free assertion helper; checks remain active without -ea. */
final class Checks {
    static int count;
    private Checks() { }

    static void equal(Object expected, Object actual) {
        count++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    static void fails(Throwing action) throws Exception {
        count++;
        try { action.run(); }
        catch (IllegalArgumentException | java.io.IOException expected) { return; }
        throw new AssertionError("Expected validation or IO error");
    }

    @FunctionalInterface
    interface Throwing { void run() throws Exception; }
}
