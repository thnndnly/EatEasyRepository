package de.eateasy.suggestion.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * {@link OllamaClient}-Implementierung gegen Groqs OpenAI-kompatible
 * Chat-Completions-API ({@code POST /openai/v1/chat/completions}). Ermöglicht
 * KI-Vorschläge dort, wo kein self-hosted Ollama läuft (z. B. Render-Demo).
 *
 * <p>Aktiv nur, wenn {@code ai.provider=groq} — die Auswahl trifft der
 * {@link LlmClientProducer}. Der Prompt (inkl. der Anweisung, mit JSON zu
 * antworten) wird als einzelne User-Message geschickt; {@code response_format:
 * json_object} erzwingt gültiges JSON — analog zu Ollamas {@code format=json}.</p>
 */
@Typed(GroqClient.class)
@ApplicationScoped
public class GroqClient implements OllamaClient {

    private static final Logger LOG = Logger.getLogger(GroqClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final Duration readTimeout;

    public GroqClient(
        ObjectMapper objectMapper,
        @ConfigProperty(name = "groq.url",
            defaultValue = "https://api.groq.com/openai/v1/chat/completions") String url,
        @ConfigProperty(name = "groq.api-key") Optional<String> apiKey,
        @ConfigProperty(name = "groq.model", defaultValue = "llama-3.3-70b-versatile") String model,
        @ConfigProperty(name = "groq.read-timeout-seconds", defaultValue = "60") int readTimeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.endpoint = URI.create(url);
        this.apiKey = apiKey.filter(k -> !k.isBlank()).orElse("");
        this.model = model;
        this.readTimeout = Duration.ofSeconds(readTimeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public OllamaGenerateResponse generate(OllamaGenerateRequest request) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("groq.api-key ist nicht gesetzt");
        }
        try {
            String body = buildRequestBody(objectMapper, model, request.prompt());
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(readTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                    "Groq responded with HTTP " + response.statusCode());
            }
            String content = parseContent(objectMapper, response.body());
            return new OllamaGenerateResponse(model, content, true);
        } catch (Exception ex) {
            // Wie OllamaHttpClient: hochwerfen — der SmartSuggestionService fängt
            // ab und signalisiert aiAvailable=false.
            LOG.errorf(ex, "Groq-Aufruf fehlgeschlagen");
            throw new RuntimeException("Groq-Call fehlgeschlagen: " + ex.getMessage(), ex);
        }
    }

    /** Baut den OpenAI-kompatiblen Chat-Request-Body (JSON-Mode erzwungen). */
    static String buildRequestBody(ObjectMapper mapper, String model, String prompt) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);
        root.put("stream", false);
        root.putObject("response_format").put("type", "json_object");
        var messages = root.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        return root.toString();
    }

    /** Extrahiert {@code choices[0].message.content} aus der Groq-Antwort. */
    static String parseContent(ObjectMapper mapper, String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isTextual() ? content.asText() : "";
        } catch (Exception ex) {
            return "";
        }
    }
}
