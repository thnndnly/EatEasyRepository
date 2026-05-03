package de.eateasy.household.resource;

import de.eateasy.common.security.CurrentUser;
import de.eateasy.household.dto.AcceptInvitationRequest;
import de.eateasy.household.dto.HouseholdDto;
import de.eateasy.household.service.HouseholdService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/invitations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class InvitationResource {

    private final HouseholdService householdService;
    private final CurrentUser currentUser;

    public InvitationResource(HouseholdService householdService, CurrentUser currentUser) {
        this.householdService = householdService;
        this.currentUser = currentUser;
    }

    @POST
    @Path("/accept")
    public HouseholdDto accept(@Valid AcceptInvitationRequest request) {
        return householdService.acceptInvitation(currentUser.id(), request.token());
    }
}
