package de.eateasy.pantry.resource;

import de.eateasy.common.security.CurrentUser;
import de.eateasy.pantry.dto.PantryItemDto;
import de.eateasy.pantry.dto.UpdatePantryItemRequest;
import de.eateasy.pantry.service.PantryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/pantry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class PantryItemResource {

    private final PantryService pantryService;
    private final CurrentUser currentUser;

    public PantryItemResource(PantryService pantryService, CurrentUser currentUser) {
        this.pantryService = pantryService;
        this.currentUser = currentUser;
    }

    @PATCH
    @Path("/{id}")
    public PantryItemDto update(@PathParam("id") UUID id, @Valid UpdatePantryItemRequest request) {
        return pantryService.update(currentUser.id(), id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        pantryService.delete(currentUser.id(), id);
        return Response.noContent().build();
    }
}
