package de.eateasy.suggestion.client;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifiziert, dass {@code ai.provider=groq} tatsächlich den {@link GroqClient}
 * aktiviert: ohne API-Key schlägt sein {@code generate} mit einem groq-
 * spezifischen Fehler fehl (der OllamaHttpClient würde stattdessen versuchen,
 * Ollama zu erreichen). Damit ist die Producer-Auswahl abgesichert, ohne einen
 * echten Groq-Aufruf zu machen.
 */
@QuarkusTest
@TestProfile(LlmProviderSelectionTest.GroqProviderProfile.class)
class LlmProviderSelectionTest {

    public static class GroqProviderProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("ai.provider", "groq");
        }
    }

    @Inject
    OllamaClient llmClient;

    @Test
    @DisplayName("ai.provider=groq → GroqClient aktiv (ohne Key: groq-spezifischer Fehler)")
    void groqSelectedWhenConfigured() {
        assertThatThrownBy(() -> llmClient.generate(OllamaGenerateRequest.of("m", "gib JSON zurück")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("groq.api-key");
    }
}
