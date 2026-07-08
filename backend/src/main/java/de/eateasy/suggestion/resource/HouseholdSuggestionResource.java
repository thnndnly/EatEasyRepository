package de.eateasy.suggestion.resource;

import de.eateasy.common.security.CurrentUser;
import de.eateasy.suggestion.dto.SuggestRequest;
import de.eateasy.suggestion.dto.SuggestionResponse;
import de.eateasy.suggestion.service.SmartSuggestionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/v1/households/{householdId}/suggestions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class HouseholdSuggestionResource {

    private final SmartSuggestionService suggestionService;
    private final CurrentUser currentUser;

    public HouseholdSuggestionResource(SmartSuggestionService suggestionService,
                                       CurrentUser currentUser) {
        this.suggestionService = suggestionService;
        this.currentUser = currentUser;
    }

    @POST
    public SuggestionResponse suggest(@PathParam("householdId") UUID householdId,
                                      @Valid SuggestRequest request) {
        return suggestionService.suggest(currentUser.id(), householdId, request.numSuggestions());
    }
}
