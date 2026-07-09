package de.eateasy.auth.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eateasy.common.exception.InvalidCredentialsException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Verifiziert Google-ID-Tokens über Googles {@code tokeninfo}-Endpoint. Google
 * prüft dort Signatur und Ablauf serverseitig; wir prüfen zusätzlich die
 * Audience gegen unsere Client-ID. Bewusst ohne zusätzliche Google-SDK-
 * Dependency — gleiche Design-Entscheidung wie beim {@code TesseractHttpClient}
 * (JDK-HttpClient, Base-URL fix). Für die erwartbaren Login-Volumina eines
 * Studienprojekts ist der Remote-Call unkritisch.
 */
@ApplicationScoped
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final Logger LOG = Logger.getLogger(GoogleTokenVerifierImpl.class);
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String clientId;

    public GoogleTokenVerifierImpl(
        ObjectMapper objectMapper,
        // Optional statt defaultValue="": SmallRye behandelt einen leeren
        // String-Wert als "nicht gesetzt" und würde die Konvertierung sonst
        // beim Boot fehlschlagen lassen.
        @ConfigProperty(name = "google.oauth.client-id") Optional<String> clientId
    ) {
        this.objectMapper = objectMapper;
        this.clientId = clientId.filter(value -> !value.isBlank()).orElse("");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public GoogleIdTokenPayload verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidCredentialsException();
        }
        if (clientId.isBlank()) {
            // Feature nicht konfiguriert — kein gültiges Token möglich.
            LOG.warn("google.oauth.client-id ist nicht gesetzt, Google-Login nicht möglich");
            throw new InvalidCredentialsException();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKENINFO_URL + "?id_token="
                    + URLEncoder.encode(idToken, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                // 400 = Token abgelaufen/ungültig — kein Server-Fehler.
                throw new InvalidCredentialsException();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String audience = root.path("aud").asText("");
            if (!clientId.equals(audience)) {
                LOG.warnf("Google-Token mit fremder Audience abgelehnt: %s", audience);
                throw new InvalidCredentialsException();
            }

            return new GoogleIdTokenPayload(
                textOrNull(root, "email"),
                Boolean.parseBoolean(root.path("email_verified").asText("false")),
                textOrNull(root, "sub"),
                textOrNull(root, "name"));
        } catch (InvalidCredentialsException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.errorf(ex, "Google-Token-Verifikation fehlgeschlagen");
            throw new InvalidCredentialsException();
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }
}
