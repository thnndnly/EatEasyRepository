package de.eateasy.integration.dto;

import de.eateasy.common.units.Unit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BarcodePantryRequest(
    @NotBlank @Size(max = 50) String barcode,
    @NotNull @DecimalMin(value = "0.01", message = "Menge muss positiv sein") BigDecimal amount,
    @NotNull Unit unit,
    LocalDate bestBefore
) {
}
