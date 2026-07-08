package de.eateasy.suggestion.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reine Unit-Tests der Request-/Response-Abbildung — ohne HTTP oder API-Key.
 */
class GroqClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("buildRequestBody: OpenAI-Chat-Form mit erzwungenem JSON-Mode")
    void buildRequestBodyProducesOpenAiChatWithJsonMode() throws Exception {
        String body = GroqClient.buildRequestBody(mapper, "llama-3.3-70b-versatile", "Antworte mit JSON");

        JsonNode root = mapper.readTree(body);
        assertThat(root.get("model").asText()).isEqualTo("llama-3.3-70b-versatile");
        assertThat(root.path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(root.path("stream").asBoolean()).isFalse();
        assertThat(root.path("messages").path(0).path("role").asText()).isEqualTo("user");
        assertThat(root.path("messages").path(0).path("content").asText())
            .isEqualTo("Antworte mit JSON");
    }

    @Test
    @DisplayName("parseContent: extrahiert choices[0].message.content")
    void parseContentExtractsAssistantMessage() {
        String groqResponse = "{\"choices\":[{\"message\":{\"role\":\"assistant\","
            + "\"content\":\"[{\\\"recipeId\\\":\\\"x\\\"}]\"}}]}";

        String content = GroqClient.parseContent(mapper, groqResponse);

        assertThat(content).isEqualTo("[{\"recipeId\":\"x\"}]");
    }

    @Test
    @DisplayName("parseContent: leerer String bei fehlendem/kaputtem Body")
    void parseContentReturnsEmptyForGarbage() {
        assertThat(GroqClient.parseContent(mapper, "kein json")).isEmpty();
        assertThat(GroqClient.parseContent(mapper, "{}")).isEmpty();
        assertThat(GroqClient.parseContent(mapper, "{\"choices\":[]}")).isEmpty();
    }
}
