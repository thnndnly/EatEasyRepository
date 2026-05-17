package de.eateasy.integration.dto;

import de.eateasy.common.units.Unit;

/**
 * Preview-Daten zu einem Barcode-Treffer. Im Frontend wird der Name angezeigt
 * und {@link #suggestedUnit} als Default in das Mengen-Eingabefeld gesetzt —
 * der User bestaetigt mit der konkreten Menge und schickt sie an
 * {@code POST /households/{id}/pantry/barcode}.
 */
public record BarcodeProductDto(
    String barcode,
    String name,
    Unit suggestedUnit
) {
}
