package de.eateasy.suggestion.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal-Mapping: wir brauchen nur das {@code response}-Feld als String
 * (enthält das vom Modell generierte JSON, wenn {@code format=json}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaGenerateResponse(
    @JsonProperty("model") String model,
    @JsonProperty("response") String response,
    @JsonProperty("done") Boolean done
) {
}
