package de.eateasy.receipt.service;

import de.eateasy.receipt.dto.ReceiptScanResponse;

import java.util.UUID;

public interface ReceiptScanService {

    /**
     * Scannt ein Belegfoto: OCR via Tesseract, Strukturierung via Ollama,
     * Ingredient-Matching gegen den globalen Zutaten-Pool. Persistiert
     * nichts — die Bestätigung läuft über die bestehende Pantry-API.
     *
     * @throws de.eateasy.common.exception.ForbiddenException  wenn der User kein Haushaltsmitglied ist
     * @throws de.eateasy.common.exception.BadRequestException wenn das Bild keinen Text ergibt
     */
    ReceiptScanResponse scan(UUID userId, UUID householdId, byte[] imageBytes, String filename);
}
