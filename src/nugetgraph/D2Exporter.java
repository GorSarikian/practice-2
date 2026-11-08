package nugetgraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public final class D2Exporter {
    private static final long RENDER_TIMEOUT_SECONDS = 90;
    private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10 };
    private D2Exporter() { }

    public static String text(Graph graph) {
        StringBuilder result = new StringBuilder("direction: right\n\n");
        Map<PackageRef, String> ids = new LinkedHashMap<>();
        for (PackageRef node : graph.nodes()) { ids.put(node, "n" + ids.size()); }
        if (ids.isEmpty()) { return result.append("empty: \"Root excluded by filter\"\n").toString(); }
        for (PackageRef node : graph.nodes()) {
            String label = node.label() + (graph.truncated(node) ? "\n[depth limit]" : "");
            result.append(ids.get(node)).append(": \"").append(escape(label)).append("\"\n");
            if (node.equals(graph.root())) { result.append(ids.get(node)).append(".style.fill: \"#DBEAFE\"\n"); }
        }
        result.append('\n');
        for (PackageRef node : graph.nodes()) {
            for (PackageRef child : graph.children(node)) {
                result.append(ids.get(node)).append(" -> ").append(ids.get(child)).append('\n');
            }
        }
        return result.toString();
    }

    public static Path export(Graph graph, Path output, String executable) throws IOException {
        Path absolute = output.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        String name = absolute.getFileName().toString();
        Path source = absolute.resolveSibling(name.substring(0, name.length() - ".png".length()) + ".d2");
        Files.writeString(source, text(graph));
        Path temporary = Files.createTempFile(absolute.getParent(), ".render-", ".png");
        Path log = Files.createTempFile("d2-render-", ".log");
        try {
            render(source, temporary, log, executable);
            var image = ImageIO.read(temporary.toFile());
            if (image == null) { throw new IOException("D2 did not produce a valid image"); }
            byte[] signature = Files.readAllBytes(temporary);
            byte[] header = java.util.Arrays.copyOf(signature, PNG_SIGNATURE.length);
            if (!java.util.Arrays.equals(header, PNG_SIGNATURE)) { throw new IOException("D2 output is not PNG"); }
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            return source;
        } finally { Files.deleteIfExists(temporary); Files.deleteIfExists(log); }
    }

    private static void render(Path source, Path output, Path log, String executable) throws IOException {
        Process process;
        try {
            process = new ProcessBuilder(executable, source.toString(), output.toString())
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        } catch (IOException exception) {
            throw new IOException("Cannot start D2. Install D2 and set D2_COMMAND to d2.exe; D2 source: "
                    + source, exception);
        }
        try {
            if (!process.waitFor(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("D2 rendering timed out");
            }
            if (process.exitValue() != 0) {
                throw new IOException("D2 exit " + process.exitValue() + ": " + Files.readString(log));
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("D2 rendering interrupted", exception);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
