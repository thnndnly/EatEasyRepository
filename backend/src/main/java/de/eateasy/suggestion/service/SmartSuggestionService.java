package de.eateasy.suggestion.service;

import de.eateasy.suggestion.dto.SuggestionDto;

import java.util.List;
import java.util.UUID;

public interface SmartSuggestionService {

    /**
     * Hybrid-Vorschlaege: Coverage-Heuristik filtert + sortiert, Ollama
     * reranked die Top-Kandidaten und liefert eine kurze Begruendung. Bei
     * Ollama-Fehler / Timeout / unparsbarem JSON faellt die Methode auf
     * die reine Coverage-Reihenfolge zurueck — die {@code reason} der DTOs
     * ist dann {@code null}.
     */
    List<SuggestionDto> suggest(UUID userId, UUID householdId, int numSuggestions);
}
