package de.eateasy.suggestion.dto;

import java.util.List;

/**
 * Antwort der Smart-Suggestion. {@code aiAvailable} signalisiert, ob die
 * KI-Reranking-/Begründungs-Stufe (Ollama) erreichbar war: bei {@code false}
 * ist die Liste rein nach Vorrats-Abdeckung sortiert und die {@code reason}-
 * Felder sind leer — das Frontend kann dann einen Hinweis anzeigen, statt die
 * stille Degradierung unbemerkt zu lassen.
 */
public record SuggestionResponse(
    boolean aiAvailable,
    List<SuggestionDto> suggestions
) {
}
