package nugetgraph;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** HTTP transport seam for offline tests. */
@FunctionalInterface
public interface HttpText {
    /** Gets UTF-8 text or throws a clear network/status error. */
    String get(URI uri) throws IOException;

    /** Creates the standard JDK transport with bounded connection and request times. */
    static HttpText standard() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        return uri -> {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Java-NuGet-Graph-Practice/1.0").GET().build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + " for " + uri);
                }
                return response.body();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("HTTP request interrupted", exception);
            }
        };
    }
}
