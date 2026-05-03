package de.eateasy.shoppinglist.resource;

import de.eateasy.common.security.CurrentUser;
import de.eateasy.shoppinglist.dto.ShoppingListItemDto;
import de.eateasy.shoppinglist.dto.ToggleCheckedRequest;
import de.eateasy.shoppinglist.service.ShoppingListService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/v1/shoppinglist/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class ShoppingListItemResource {

    private final ShoppingListService shoppingListService;
    private final CurrentUser currentUser;

    public ShoppingListItemResource(ShoppingListService shoppingListService,
                                    CurrentUser currentUser) {
        this.shoppingListService = shoppingListService;
        this.currentUser = currentUser;
    }

    @PATCH
    @Path("/{id}")
    public ShoppingListItemDto toggle(@PathParam("id") UUID id,
                                      @Valid ToggleCheckedRequest request) {
        return shoppingListService.toggleChecked(currentUser.id(), id, request.checked());
    }
}
