package de.eateasy.receipt.dto;

import de.eateasy.common.units.Unit;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ein aus dem Beleg erkannter Posten (Vorschau, noch nicht persistiert).
 * {@code ingredientId} ist gesetzt, wenn eine bestehende Zutat mit gleichem
 * Namen gefunden wurde — sonst legt die Bestaetigung eine neue an.
 */
public record ReceiptItemDto(
    String name,
    BigDecimal amount,
    Unit unit,
    UUID ingredientId
) {
}
