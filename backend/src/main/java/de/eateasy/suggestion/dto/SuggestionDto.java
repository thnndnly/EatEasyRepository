package de.eateasy.suggestion.dto;

import de.eateasy.recipe.dto.RecipeMiniDto;

/**
 * Ein Smart-Suggestion-Vorschlag. {@code reason} kann {@code null} sein,
 * wenn der Ollama-Fallback aktiv war — das Frontend zeigt dann statt
 * Begründung einen Hinweis auf die Coverage-Heuristik.
 */
public record SuggestionDto(
    RecipeMiniDto recipe,
    String reason,
    double coverage
) {
}
