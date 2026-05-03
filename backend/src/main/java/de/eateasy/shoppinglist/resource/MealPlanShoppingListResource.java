package de.eateasy.shoppinglist.resource;

import de.eateasy.common.security.CurrentUser;
import de.eateasy.shoppinglist.dto.ShoppingListDto;
import de.eateasy.shoppinglist.service.ShoppingListService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

/**
 * Endpoints, die einen MealPlan im Pfad haben — Lazy-Generierung und
 * Regenerate. Operationen auf einzelnen Items leben in
 * {@link ShoppingListItemResource}.
 */
@Path("/api/v1/mealplans/{mealPlanId}/shoppinglist")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class MealPlanShoppingListResource {

    private final ShoppingListService shoppingListService;
    private final CurrentUser currentUser;

    public MealPlanShoppingListResource(ShoppingListService shoppingListService,
                                        CurrentUser currentUser) {
        this.shoppingListService = shoppingListService;
        this.currentUser = currentUser;
    }

    @GET
    public ShoppingListDto getOrGenerate(@PathParam("mealPlanId") UUID mealPlanId) {
        return shoppingListService.getOrGenerate(currentUser.id(), mealPlanId);
    }

    @POST
    @Path("/regenerate")
    public ShoppingListDto regenerate(@PathParam("mealPlanId") UUID mealPlanId) {
        return shoppingListService.regenerate(currentUser.id(), mealPlanId);
    }
}
