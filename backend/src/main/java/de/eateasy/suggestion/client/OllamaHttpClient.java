package de.eateasy.suggestion.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Direkt-Aufruf gegen Ollama mit dem JDK-{@link HttpClient}. Vorteil ggue. dem
 * MP-REST-Client: kein magic property lookup — Base-URL wird beim Bean-Init
 * einmal aufgelöst und steht. Timeouts werden explizit gesetzt; der
 * SmartSuggestionService fängt Exceptions ab und fa-llt auf Coverage zurück.
 */
// @Typed: nur als konkreter Typ injizierbar, nicht als OllamaClient — die
// OllamaClient-Auswahl (Ollama vs. Groq) übernimmt der LlmClientProducer.
@Typed(OllamaHttpClient.class)
@ApplicationScoped
public class OllamaHttpClient implements OllamaClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI generateUri;
    private final Duration readTimeout;

    public OllamaHttpClient(
        ObjectMapper objectMapper,
        @ConfigProperty(name = "ollama.url", defaultValue = "http://localhost:11434") String baseUrl,
        @ConfigProperty(name = "ollama.read-timeout-seconds", defaultValue = "60") int readTimeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.generateUri = URI.create(baseUrl + "/api/generate");
        this.readTimeout = Duration.ofSeconds(readTimeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public OllamaGenerateResponse generate(OllamaGenerateRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(generateUri)
                .timeout(readTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = httpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new WebApplicationException(
                    "Ollama responded with HTTP " + response.statusCode() + ": "
                        + response.body(), response.statusCode());
            }
            return objectMapper.readValue(response.body(), OllamaGenerateResponse.class);
        } catch (Exception ex) {
            // RuntimeException, damit das Service-Catch greift und auf Coverage fällt.
            throw new RuntimeException("Ollama-Call fehlgeschlagen: " + ex.getMessage(), ex);
        }
    }
}
