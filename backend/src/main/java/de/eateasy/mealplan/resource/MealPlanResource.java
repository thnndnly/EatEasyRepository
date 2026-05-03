package de.eateasy.mealplan.resource;

import de.eateasy.common.security.CurrentUser;
import de.eateasy.mealplan.dto.MealPlanDto;
import de.eateasy.mealplan.dto.MealPlanEntryDto;
import de.eateasy.mealplan.dto.SetEntryRequest;
import de.eateasy.mealplan.entity.MealType;
import de.eateasy.mealplan.service.MealPlanService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

@Path("/api/v1")
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

    @GET
    @Path("/households/{householdId}/mealplans")
    public MealPlanDto getOrCreate(@PathParam("householdId") UUID householdId,
                                   @QueryParam("weekStart") String weekStart) {
        LocalDate parsed = weekStart == null || weekStart.isBlank()
            ? null
            : LocalDate.parse(weekStart);
        return mealPlanService.getOrCreate(currentUser.id(), householdId, parsed);
    }

    @PUT
    @Path("/mealplans/{id}/entries")
    public MealPlanEntryDto setEntry(@PathParam("id") UUID id, @Valid SetEntryRequest request) {
        return mealPlanService.setEntry(currentUser.id(), id, request);
    }

    @DELETE
    @Path("/mealplans/{id}/entries/{day}/{mealType}")
    public Response removeEntry(@PathParam("id") UUID id,
                                @PathParam("day") DayOfWeek day,
                                @PathParam("mealType") MealType mealType) {
        mealPlanService.removeEntry(currentUser.id(), id, day, mealType);
        return Response.noContent().build();
    }
}
