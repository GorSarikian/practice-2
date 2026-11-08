package nugetgraph;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class NugetTests {
    private NugetTests() { }

    static void run() throws Exception {
        versions();
        xml();
        protocol();
        jsonEscapes();
        Checks.equal("А", ((Map<?, ?>) Json.parse("{\"x\":\"\\u0410\"}")).get("x"));
        for (String invalid : List.of("{}junk", "{\"x\":1,}", "[1,]", "\"a\n\"", "01", "{\"x\":0,\"x\":1}")) {
            Checks.fails(() -> Json.parse(invalid));
        }
    }

    private static void jsonEscapes() throws Exception {
        Checks.equal("\"\\/\b\f\n\r\t", Json.parse("\"\\\"\\\\\\/\\b\\f\\n\\r\\t\""));
        Checks.equal("A", Json.parse("\"\\u0041\""));
        for (String invalid : List.of("\"\\x\"", "\"\\", "\"\\u12\"", "\"\\uXXXX\"")) {
            Checks.fails(() -> Json.parse(invalid));
        }
    }

    private static void versions() throws Exception {
        Checks.equal("1.2.0", new Version("01.2.0.0+build").toString());
        Checks.equal(true, new Version("1.0.0-alpha.2").compareTo(new Version("1.0.0-alpha.10")) < 0);
        Checks.equal(true, new Version("1.0.0-rc").compareTo(new Version("1.0.0")) < 0);
        Checks.equal(true, new Version("1.0.0").compareTo(new Version("1.0.0-rc")) > 0);
        Checks.equal(true, new VersionRange("[1.0,2.0)").contains(new Version("1.9")));
        Checks.equal(false, new VersionRange("[1.0,2.0)").contains(new Version("2")));
        Checks.equal(true, new VersionRange("(,2.0]").contains(new Version("2")));
        Checks.equal(false, new VersionRange("(1,)").contains(new Version("1")));
        Checks.equal(true, new VersionRange("[1.0]").contains(new Version("1")));
        Checks.equal(true, new VersionRange("1.2").contains(new Version("3")));
        Checks.equal(false, new VersionRange("").contains(new Version("3-rc")));
        Checks.equal(true, new VersionRange("[3-rc,3]").contains(new Version("3-rc")));
        for (String invalid : List.of("(1)", "[2,1]", "[1,2,3]", "1.*", "[1,2")) {
            Checks.fails(() -> new VersionRange(invalid));
        }
    }

    private static void xml() throws Exception {
        String xml = "<package xmlns='urn:test'><metadata><dependencies>"
                + "<group targetFramework='.NETStandard2.0'><dependency id='B' version='[1,2)'/></group>"
                + "<group targetFramework='net8.0'/></dependencies></metadata></package>";
        Checks.equal(List.of(), Nuspec.read(xml, new Framework("net8.0")));
        Checks.equal(List.of(new Nuspec.Requirement("B", "[1,2)")),
                Nuspec.read(xml, new Framework("net6.0")));
        Checks.equal(List.of(), Nuspec.read("<package/>", new Framework("net8.0")));
        Checks.fails(() -> Nuspec.read("<!DOCTYPE p SYSTEM 'file:///missing'><package/>", new Framework("net8.0")));
        Checks.fails(() -> new Framework("net48"));
        Checks.fails(() -> new Framework("net8.0-windows"));
        Checks.equal(true, new Framework("net8.0").score("net8.0")
                > new Framework("net8.0").score(".NETStandard2.0"));
    }

    private static void protocol() throws Exception {
        Config config = Config.load(Path.of("examples/newtonsoft.toml"));
        Map<String, String> replies = Map.of(
                config.repository(), "{\"resources\":[{\"@type\":\"PackageBaseAddress/3.0.0\","
                        + "\"@id\":\"https://example.test/flat/\"}]}",
                "https://example.test/flat/a/index.json", "{\"versions\":[\"1.0\",\"2.0\",\"3.0-rc\"]}",
                "https://example.test/flat/b/index.json", "{\"versions\":[\"1.0\",\"1.5\"]}",
                "https://example.test/flat/a/2.0.0/a.nuspec", "<package><metadata><dependencies>"
                        + "<dependency id='B' version='[1,2)'/></dependencies></metadata></package>");
        NugetRepository repo = new NugetRepository(config, uri -> {
            String value = replies.get(uri.toString());
            if (value == null) { throw new IOException("HTTP 404 fixture"); }
            return value;
        });
        PackageRef root = repo.resolve("A", "latest");
        Checks.equal(new PackageRef("a", "2.0.0"), root);
        Checks.equal(List.of(new PackageRef("b", "1.0.0")), repo.dependencies(root));
        Checks.equal(repo.dependencies(root), repo.dependencies(root));
        Checks.fails(() -> repo.resolve("missing", "latest"));
        Checks.fails(() -> repo.resolve("A", "[4]"));
        Checks.fails(() -> new NugetRepository(config, uri -> "{}"));
        Checks.fails(() -> new NugetRepository(config, uri -> { throw new IOException("timeout"); }));
    }
}
