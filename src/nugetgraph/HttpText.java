package nugetgraph;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@FunctionalInterface
public interface HttpText {
    public String get(URI uri) throws IOException;

    public static HttpText standard() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        return uri -> {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Java-NuGet-Graph-Practice/1.0").GET().build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
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
