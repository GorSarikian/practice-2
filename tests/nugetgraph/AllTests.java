package nugetgraph;

/** Executes offline tests without external testing libraries. */
public final class AllTests {
    private AllTests() { }

    /** Runs all registered test suites and reports the assertion count. */
    public static void main(String[] args) throws Exception {
        ConfigTests.run();
        NugetTests.run();
        GraphTests.run();
        System.out.println("PASS: " + Checks.count + " assertions");
    }
}
