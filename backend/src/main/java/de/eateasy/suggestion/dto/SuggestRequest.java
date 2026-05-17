package de.eateasy.suggestion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SuggestRequest(
    @Min(value = 1, message = "Mindestens 1 Vorschlag")
    @Max(value = 10, message = "Maximal 10 Vorschlaege")
    int numSuggestions
) {
}
