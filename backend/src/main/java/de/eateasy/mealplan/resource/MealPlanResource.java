package de.eateasy.mealplan.resource;

import de.eateasy.common.security.CurrentUser;
import de.eateasy.mealplan.dto.MealPlanEntryDto;
import de.eateasy.mealplan.dto.SetEntryRequest;
import de.eateasy.mealplan.entity.MealType;
import de.eateasy.mealplan.service.MealPlanService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.DayOfWeek;
import java.util.UUID;

/**
 * Operationen auf einem konkreten Wochenplan. Der haushaltsbezogene
 * GET-Endpoint sitzt in {@link HouseholdMealPlanResource}, weil RESTEasy
 * Reactive eine eigene Resource-Klasse pro Class-Level-{@code @Path} braucht.
 */
@Path("/api/v1/mealplans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class MealPlanResource {

    private final MealPlanService mealPlanService;
    private final CurrentUser currentUser;

    public MealPlanResource(MealPlanService mealPlanService, CurrentUser currentUser) {
        this.mealPlanService = mealPlanService;
        this.currentUser = currentUser;
    }

    @PUT
    @Path("/{id}/entries")
    public MealPlanEntryDto setEntry(@PathParam("id") UUID id, @Valid SetEntryRequest request) {
        return mealPlanService.setEntry(currentUser.id(), id, request);
    }

    @DELETE
    @Path("/{id}/entries/{day}/{mealType}")
    public Response removeEntry(@PathParam("id") UUID id,
                                @PathParam("day") DayOfWeek day,
                                @PathParam("mealType") MealType mealType) {
        mealPlanService.removeEntry(currentUser.id(), id, day, mealType);
        return Response.noContent().build();
    }
}
