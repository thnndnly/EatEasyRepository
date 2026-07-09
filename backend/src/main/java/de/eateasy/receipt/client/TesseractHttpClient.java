package de.eateasy.receipt.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Client für den tesseract-server-Container (hertzg/tesseract-server):
 * {@code POST /tesseract} mit Multipart-Feldern {@code options} (JSON) und
 * {@code file}. Antwort: {@code {"data":{"stdout":"<text>", ...}}}.
 *
 * <p>Multipart wird von Hand gebaut (JDK-HttpClient hat keinen Builder dafür)
 * — gleiche Design-Entscheidung wie beim {@code OllamaHttpClient}: kein
 * MP-REST-Client-Magic, Base-URL steht beim Bean-Init fest.</p>
 */
@ApplicationScoped
public class TesseractHttpClient implements OcrClient {

    private static final String OPTIONS_JSON = "{\"languages\":[\"deu\",\"eng\"]}";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI tesseractUri;
    private final Duration readTimeout;

    public TesseractHttpClient(
        ObjectMapper objectMapper,
        @ConfigProperty(name = "tesseract.url", defaultValue = "http://localhost:8884") String baseUrl,
        @ConfigProperty(name = "tesseract.read-timeout-seconds", defaultValue = "30") int readTimeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.tesseractUri = URI.create(baseUrl + "/tesseract");
        this.readTimeout = Duration.ofSeconds(readTimeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public String extractText(byte[] imageBytes, String filename) {
        try {
            String boundary = "----eateasy-" + UUID.randomUUID();
            byte[] body = buildMultipartBody(boundary, imageBytes, filename);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(tesseractUri)
                .timeout(readTimeout)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                    "Tesseract responded with HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode stdout = root.path("data").path("stdout");
            return stdout.isTextual() ? stdout.asText() : "";
        } catch (Exception ex) {
            throw new RuntimeException("Tesseract-Call fehlgeschlagen: " + ex.getMessage(), ex);
        }
    }

    private static byte[] buildMultipartBody(String boundary, byte[] imageBytes, String filename)
        throws java.io.IOException {
        // Der Dateiname landet roh im Multipart-Header — neben Anführungszeichen
        // auch CR/LF entfernen, damit ein präparierter Name keine zusätzlichen
        // Header/Parts in den Request an Tesseract einschleusen kann.
        String safeName = filename == null || filename.isBlank()
            ? "receipt.jpg"
            : filename.replaceAll("[\"\\r\\n]", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writeAscii(out, "--" + boundary + "\r\n");
        writeAscii(out, "Content-Disposition: form-data; name=\"options\"\r\n\r\n");
        writeAscii(out, OPTIONS_JSON + "\r\n");

        writeAscii(out, "--" + boundary + "\r\n");
        writeAscii(out, "Content-Disposition: form-data; name=\"file\"; filename=\""
            + safeName + "\"\r\n");
        writeAscii(out, "Content-Type: application/octet-stream\r\n\r\n");
        out.write(imageBytes);
        writeAscii(out, "\r\n--" + boundary + "--\r\n");

        return out.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) throws java.io.IOException {
        out.write(s.getBytes(StandardCharsets.US_ASCII));
    }
}
