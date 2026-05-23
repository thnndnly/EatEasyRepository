package de.eateasy.recipe.resource;

import de.eateasy.common.security.CurrentUser;
import de.eateasy.integration.dto.RecipeImportRequest;
import de.eateasy.integration.service.RecipeImportService;
import de.eateasy.recipe.dto.RecipeCreateRequest;
import de.eateasy.recipe.dto.RecipeDto;
import de.eateasy.recipe.dto.RecipeFilter;
import de.eateasy.recipe.dto.RecipeUpdateRequest;
import de.eateasy.recipe.service.RecipeService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/recipes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@Tag(name = "Rezepte", description = "Verwaltung von Haushalts-Rezepten inkl. Diät-Tags und Import aus externen Quellen.")
@SecurityRequirement(name = "BearerAuth")
public class RecipeResource {

    private final RecipeService recipeService;
    private final RecipeImportService recipeImportService;
    private final CurrentUser currentUser;

    public RecipeResource(RecipeService recipeService,
                          RecipeImportService recipeImportService,
                          CurrentUser currentUser) {
        this.recipeService = recipeService;
        this.recipeImportService = recipeImportService;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(summary = "Rezepte auflisten", description = "Liefert alle Rezepte, optional gefiltert nach Suchtext, Diät-Tags und Haushalt.")
    @APIResponse(responseCode = "200", description = "Liste der Rezepte (kann leer sein).")
    public List<RecipeDto> list(@Parameter(description = "Freitext-Suche (Titel, Zutaten).") @QueryParam("q") String query,
                                @Parameter(description = "Diät-Tags kommasepariert, z. B. 'vegan,glutenfrei'.") @QueryParam("dietTags") String dietTags,
                                @Parameter(description = "Auf Haushalt einschränken.") @QueryParam("householdId") UUID householdId) {
        List<String> tags = parseDietTags(dietTags);
        RecipeFilter filter = new RecipeFilter(query, tags, householdId);
        return recipeService.list(currentUser.id(), filter);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Einzelnes Rezept laden")
    @APIResponse(responseCode = "200", description = "Rezept gefunden.")
    @APIResponse(responseCode = "404", description = "Rezept nicht vorhanden oder kein Zugriff.")
    public RecipeDto get(@Parameter(description = "Rezept-UUID.") @PathParam("id") UUID id) {
        return recipeService.get(currentUser.id(), id);
    }

    @POST
    @Operation(summary = "Rezept anlegen")
    @APIResponse(responseCode = "201", description = "Rezept erstellt.")
    @APIResponse(responseCode = "400", description = "Validierung fehlgeschlagen.")
    public Response create(@Valid RecipeCreateRequest request) {
        RecipeDto dto = recipeService.create(currentUser.id(), request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @POST
    @Path("/import")
    public Response importRecipe(@Valid RecipeImportRequest request) {
        RecipeDto dto = recipeImportService.importRecipe(currentUser.id(), request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @PATCH
    @Path("/{id}")
    public RecipeDto update(@PathParam("id") UUID id, @Valid RecipeUpdateRequest request) {
        return recipeService.update(currentUser.id(), id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        recipeService.delete(currentUser.id(), id);
        return Response.noContent().build();
    }

    private static List<String> parseDietTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
