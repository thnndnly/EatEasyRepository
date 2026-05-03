package de.eateasy.household.resource;

import de.eateasy.common.security.CurrentUser;
import de.eateasy.household.dto.HouseholdCreateRequest;
import de.eateasy.household.dto.HouseholdDto;
import de.eateasy.household.dto.HouseholdUpdateRequest;
import de.eateasy.household.dto.InvitationCreateRequest;
import de.eateasy.household.dto.InvitationDto;
import de.eateasy.household.dto.MemberDto;
import de.eateasy.household.service.HouseholdService;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/households")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class HouseholdResource {

    private final HouseholdService householdService;
    private final CurrentUser currentUser;

    public HouseholdResource(HouseholdService householdService, CurrentUser currentUser) {
        this.householdService = householdService;
        this.currentUser = currentUser;
    }

    @POST
    public Response create(@Valid HouseholdCreateRequest request) {
        HouseholdDto dto = householdService.create(currentUser.id(), request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @GET
    public List<HouseholdDto> list() {
        return householdService.listForUser(currentUser.id());
    }

    @GET
    @Path("/{id}")
    public HouseholdDto get(@PathParam("id") UUID id) {
        return householdService.get(currentUser.id(), id);
    }

    @PATCH
    @Path("/{id}")
    public HouseholdDto update(@PathParam("id") UUID id, @Valid HouseholdUpdateRequest request) {
        return householdService.update(currentUser.id(), id, request);
    }

    @POST
    @Path("/{id}/invitations")
    public Response invite(@PathParam("id") UUID id, @Valid InvitationCreateRequest request) {
        InvitationDto dto = householdService.invite(currentUser.id(), id, request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @GET
    @Path("/{id}/members")
    public List<MemberDto> members(@PathParam("id") UUID id) {
        return householdService.listMembers(currentUser.id(), id);
    }

    @DELETE
    @Path("/{id}/members/{memberId}")
    public Response removeMember(@PathParam("id") UUID id, @PathParam("memberId") UUID memberId) {
        householdService.removeMember(currentUser.id(), id, memberId);
        return Response.noContent().build();
    }
}
