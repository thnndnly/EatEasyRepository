package de.eateasy.recipe.resource;

import de.eateasy.common.security.CurrentUser;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/recipes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class RecipeResource {

    private final RecipeService recipeService;
    private final CurrentUser currentUser;

    public RecipeResource(RecipeService recipeService, CurrentUser currentUser) {
        this.recipeService = recipeService;
        this.currentUser = currentUser;
    }

    @GET
    public List<RecipeDto> list(@QueryParam("q") String query,
                                @QueryParam("dietTags") String dietTags,
                                @QueryParam("householdId") UUID householdId) {
        List<String> tags = parseDietTags(dietTags);
        RecipeFilter filter = new RecipeFilter(query, tags, householdId);
        return recipeService.list(currentUser.id(), filter);
    }

    @GET
    @Path("/{id}")
    public RecipeDto get(@PathParam("id") UUID id) {
        return recipeService.get(currentUser.id(), id);
    }

    @POST
    public Response create(@Valid RecipeCreateRequest request) {
        RecipeDto dto = recipeService.create(currentUser.id(), request);
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
