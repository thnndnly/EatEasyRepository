package de.eateasy.integration.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client gegen TheMealDB (gratis, kein API-Key — public ID "1").
 * Base-URL kommt aus {@code quarkus.rest-client.themealdb.url}.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/json/v1/1/search.php?s=<query>} — Volltextsuche</li>
 *   <li>{@code GET /api/json/v1/1/lookup.php?i=<idMeal>} — Detail-Lookup</li>
 * </ul>
 */
@RegisterRestClient(configKey = "themealdb")
@Path("/api/json/v1/1")
@Produces(MediaType.APPLICATION_JSON)
public interface TheMealDbClient {

    @GET
    @Path("/search.php")
    TheMealDbResponse search(@QueryParam("s") String query);

    @GET
    @Path("/lookup.php")
    TheMealDbResponse lookup(@QueryParam("i") String idMeal);
}
