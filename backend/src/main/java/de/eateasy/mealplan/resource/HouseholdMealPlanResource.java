package de.eateasy.mealplan.resource;

import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.security.CurrentUser;
import de.eateasy.mealplan.dto.MealPlanDto;
import de.eateasy.mealplan.service.MealPlanService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Eigene Resource-Klasse, weil REST-Resources in RESTEasy einen einzigen
 * Class-Level-{@code @Path} brauchen. Der haushaltsbezogene GET-Endpoint
 * lebt unter {@code /api/v1/households/{id}/mealplans}, alle anderen
 * MealPlan-Operationen unter {@code /api/v1/mealplans} —
 * siehe {@link MealPlanResource}.
 */
@Path("/api/v1/households/{householdId}/mealplans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class HouseholdMealPlanResource {

    private final MealPlanService mealPlanService;
    private final CurrentUser currentUser;

    public HouseholdMealPlanResource(MealPlanService mealPlanService, CurrentUser currentUser) {
        this.mealPlanService = mealPlanService;
        this.currentUser = currentUser;
    }

    @GET
    public MealPlanDto getOrCreate(@PathParam("householdId") UUID householdId,
                                   @QueryParam("weekStart") String weekStart) {
        LocalDate parsed = parseWeekStart(weekStart);
        return mealPlanService.getOrCreate(currentUser.id(), householdId, parsed);
    }

    private static LocalDate parseWeekStart(String weekStart) {
        if (weekStart == null || weekStart.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(weekStart);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(
                "Ungueltiges weekStart-Format, erwartet ISO-Datum YYYY-MM-DD: " + weekStart);
        }
    }
}
