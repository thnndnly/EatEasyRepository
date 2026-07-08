package de.eateasy.suggestion.service;

import de.eateasy.suggestion.dto.SuggestionResponse;

import java.util.UUID;

public interface SmartSuggestionService {

    /**
     * Hybrid-Vorschlaege: Coverage-Heuristik filtert + sortiert, Ollama
     * reranked die Top-Kandidaten und liefert eine kurze Begruendung. Bei
     * Ollama-Fehler / Timeout / unparsbarem JSON faellt die Methode auf die
     * reine Coverage-Reihenfolge zurueck — {@code reason} ist dann {@code null}
     * und {@link SuggestionResponse#aiAvailable()} ist {@code false}.
     */
    SuggestionResponse suggest(UUID userId, UUID householdId, int numSuggestions);
}
