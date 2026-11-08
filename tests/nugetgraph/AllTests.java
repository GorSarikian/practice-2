package nugetgraph;

public final class AllTests {
    private AllTests() { }

    public static void main(String[] args) throws Exception {
        ConfigTests.run();
        NugetTests.run();
        GraphTests.run();
        ReverseTests.run();
        VisualizationTests.run();
        SourceRules.run();
        System.out.println("PASS: " + Checks.count + " assertions");
    }
}
