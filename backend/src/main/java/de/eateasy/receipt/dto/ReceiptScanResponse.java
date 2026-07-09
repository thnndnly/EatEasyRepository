package de.eateasy.receipt.dto;

import java.util.List;

/**
 * Ergebnis eines Beleg-Scans: der OCR-Rohtext (für Transparenz/Debugging im
 * UI) und die von der LLM-Strukturierung extrahierten Posten. {@code items}
 * kann leer sein, wenn Ollama nichts erkannt hat oder nicht erreichbar war —
 * der Rohtext ist dann trotzdem da.
 */
public record ReceiptScanResponse(
    String rawText,
    List<ReceiptItemDto> items
) {
}
