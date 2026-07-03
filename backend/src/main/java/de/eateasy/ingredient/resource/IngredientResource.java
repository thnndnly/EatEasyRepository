package de.eateasy.ingredient.resource;

import de.eateasy.ingredient.dto.IngredientCreateRequest;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.dto.IngredientUpdateRequest;
import de.eateasy.ingredient.service.IngredientService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/ingredients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class IngredientResource {

    private final IngredientService ingredientService;

    public IngredientResource(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GET
    public List<IngredientDto> search(@QueryParam("q") String query,
                                      @QueryParam("limit") @DefaultValue("20") int limit) {
        return ingredientService.search(query, limit);
    }

    @GET
    @Path("/{id}")
    public IngredientDto get(@PathParam("id") UUID id) {
        return ingredientService.getById(id);
    }

    @POST
    public Response create(@Valid IngredientCreateRequest request) {
        IngredientDto dto = ingredientService.findOrCreate(request.name(), request.defaultUnit());
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @PATCH
    @Path("/{id}")
    public IngredientDto update(@PathParam("id") UUID id, @Valid IngredientUpdateRequest request) {
        return ingredientService.updateCategory(id, request.category());
    }
}
