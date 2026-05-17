package de.eateasy.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal-Mapping fuer die OpenFoodFacts-v2-Antwort. Das echte JSON ist
 * riesig — wir mappen nur, was wir brauchen, und ignorieren den Rest.
 *
 * <p>{@code status = 0} signalisiert „nicht gefunden". Ansonsten steht das
 * Produkt im {@code product}-Sub-Objekt.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenFoodFactsResponse(
    @JsonProperty("code") String code,
    @JsonProperty("status") Integer status,
    @JsonProperty("product") Product product
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(
        @JsonProperty("product_name") String productName,
        @JsonProperty("product_name_de") String productNameDe,
        @JsonProperty("quantity") String quantity
    ) {
    }
}
