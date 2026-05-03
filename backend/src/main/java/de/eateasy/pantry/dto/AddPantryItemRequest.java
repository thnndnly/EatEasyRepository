package de.eateasy.pantry.dto;

import de.eateasy.common.units.Unit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Eingabe fuer das Hinzufuegen eines Vorrats-Eintrags. Entweder existierende
 * {@code ingredientId} oder neuer {@code ingredientName} (wird via
 * findOrCreate angelegt). Wenn schon ein Slot mit derselben Zutat + Unit
 * existiert, wird die Menge addiert.
 */
public record AddPantryItemRequest(
    UUID ingredientId,
    @Size(max = 100) String ingredientName,
    @NotNull @DecimalMin(value = "0.01", message = "Menge muss positiv sein") BigDecimal amount,
    @NotNull Unit unit,
    LocalDate bestBefore
) {
}
