package de.eateasy.suggestion.client;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Payload fuer {@code POST /api/generate}. {@code stream=false} aktiviert den
 * Response-Modus mit einer einzigen Antwort statt Token-Streams; {@code
 * format="json"} zwingt Ollama auf gueltiges JSON im {@code response}-Feld.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaGenerateRequest(
    String model,
    String prompt,
    Boolean stream,
    String format
) {
    public static OllamaGenerateRequest of(String model, String prompt) {
        return new OllamaGenerateRequest(model, prompt, Boolean.FALSE, "json");
    }
}
