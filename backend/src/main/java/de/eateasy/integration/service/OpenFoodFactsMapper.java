package de.eateasy.integration.service;

import de.eateasy.common.units.Unit;
import de.eateasy.common.units.UnitParser;
import de.eateasy.common.units.UnitParser.UnitParseResult;
import de.eateasy.integration.client.OpenFoodFactsResponse;
import de.eateasy.integration.client.OpenFoodFactsResponse.Product;
import de.eateasy.integration.dto.BarcodeProductDto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure Funktion: OpenFoodFacts-Antwort → {@link BarcodeProductDto}. Keine
 * CDI-Abhaengigkeiten, damit der Mapping-Code als reiner Unit-Test ohne
 * Quarkus-Boot pruefbar bleibt.
 *
 * <p>Returnt {@code null}, wenn das Produkt nicht gefunden wurde — der
 * aufrufende Service entscheidet, ob daraus eine 404 wird.</p>
 */
public final class OpenFoodFactsMapper {

    /** Liest die fuehrende Zahl + Einheit aus {@code "400 g"}, {@code "1.5 l"} etc. */
    private static final Pattern QUANTITY = Pattern.compile(
        "\\d+(?:[\\.,]\\d+)?\\s*(?<unit>[a-zA-Z]+)");

    private OpenFoodFactsMapper() {
    }

    public static BarcodeProductDto toDto(String barcode, OpenFoodFactsResponse response) {
        if (response == null || response.product() == null) {
            return null;
        }
        if (response.status() != null && response.status() == 0) {
            return null;
        }
        Product product = response.product();
        String name = pickName(product, barcode);
        Unit unit = deriveUnit(product.quantity());
        return new BarcodeProductDto(barcode, name, unit);
    }

    private static String pickName(Product product, String barcode) {
        if (product.productNameDe() != null && !product.productNameDe().isBlank()) {
            return product.productNameDe().trim();
        }
        if (product.productName() != null && !product.productName().isBlank()) {
            return product.productName().trim();
        }
        return "Produkt " + barcode;
    }

    static Unit deriveUnit(String quantity) {
        if (quantity == null || quantity.isBlank()) {
            return Unit.PIECE;
        }
        Matcher m = QUANTITY.matcher(quantity);
        if (!m.find()) {
            return Unit.PIECE;
        }
        // OpenFoodFacts speichert nur die Verpackungsgroesse als Anzeige —
        // wir interessieren uns hier nur fuer die kanonische Einheit, nicht
        // fuer den Multiplier. {@code cl} ist im Parser nicht abgebildet
        // und faellt damit auf den Default zurueck, was historischem
        // Verhalten widerspraeche; deshalb hier explizit als Sonderfall.
        String token = m.group("unit").trim().toLowerCase(java.util.Locale.ROOT);
        if ("cl".equals(token)) {
            return Unit.ML;
        }
        UnitParseResult parsed = UnitParser.parse(token, Unit.PIECE);
        return parsed.unit();
    }
}
