package de.eateasy.integration.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client gegen die OpenFoodFacts-v2-API. Base-URL via
 * {@code quarkus.rest-client.openfoodfacts.url}.
 *
 * <p>OFF antwortet immer mit {@code 200}, auch bei unbekanntem Barcode —
 * der Statuscode steckt im Body als {@code status: 0|1}.</p>
 */
@RegisterRestClient(configKey = "openfoodfacts")
@Produces(MediaType.APPLICATION_JSON)
public interface OpenFoodFactsClient {

    @GET
    @Path("/api/v2/product/{barcode}.json")
    OpenFoodFactsResponse getProduct(@PathParam("barcode") String barcode);
}
