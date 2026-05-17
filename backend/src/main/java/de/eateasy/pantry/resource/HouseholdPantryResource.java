package de.eateasy.pantry.resource;

import de.eateasy.common.security.CurrentUser;
import de.eateasy.integration.dto.BarcodePantryRequest;
import de.eateasy.integration.service.BarcodeService;
import de.eateasy.pantry.dto.AddPantryItemRequest;
import de.eateasy.pantry.dto.PantryItemDto;
import de.eateasy.pantry.service.PantryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

/**
 * Haushaltsbezogene Pantry-Endpoints. {@link PantryItemResource} kuemmert
 * sich um Operationen auf einem konkreten Item — getrennt, weil RESTEasy
 * Reactive einen einzigen Class-Level-{@code @Path} pro Resource verlangt.
 */
@Path("/api/v1/households/{householdId}/pantry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class HouseholdPantryResource {

    private final PantryService pantryService;
    private final BarcodeService barcodeService;
    private final CurrentUser currentUser;

    public HouseholdPantryResource(PantryService pantryService,
                                   BarcodeService barcodeService,
                                   CurrentUser currentUser) {
        this.pantryService = pantryService;
        this.barcodeService = barcodeService;
        this.currentUser = currentUser;
    }

    @GET
    public List<PantryItemDto> list(@PathParam("householdId") UUID householdId) {
        return pantryService.list(currentUser.id(), householdId);
    }

    @POST
    public Response add(@PathParam("householdId") UUID householdId,
                        @Valid AddPantryItemRequest request) {
        PantryItemDto dto = pantryService.add(currentUser.id(), householdId, request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @POST
    @Path("/barcode")
    public Response addByBarcode(@PathParam("householdId") UUID householdId,
                                 @Valid BarcodePantryRequest request) {
        PantryItemDto dto = barcodeService.addToPantry(currentUser.id(), householdId, request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }
}
