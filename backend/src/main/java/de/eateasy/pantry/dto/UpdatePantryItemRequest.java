package de.eateasy.pantry.dto;

import de.eateasy.common.units.Unit;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Patch-Request: nur gesetzte Felder werden übernommen.
 */
public record UpdatePantryItemRequest(
    @DecimalMin(value = "0.01", message = "Menge muss positiv sein") BigDecimal amount,
    Unit unit,
    LocalDate bestBefore
) {
}
