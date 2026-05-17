package de.eateasy.integration.resource;

import de.eateasy.integration.dto.BarcodeProductDto;
import de.eateasy.integration.dto.ExternalRecipePreviewDto;
import de.eateasy.integration.service.BarcodeService;
import de.eateasy.integration.service.RecipeImportService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/integration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class IntegrationResource {

    private final RecipeImportService recipeImportService;
    private final BarcodeService barcodeService;

    public IntegrationResource(RecipeImportService recipeImportService,
                               BarcodeService barcodeService) {
        this.recipeImportService = recipeImportService;
        this.barcodeService = barcodeService;
    }

    @GET
    @Path("/recipes/search")
    public List<ExternalRecipePreviewDto> search(
        @QueryParam("source") @DefaultValue("themealdb") String source,
        @QueryParam("q") String query
    ) {
        return recipeImportService.search(source, query);
    }

    @GET
    @Path("/products/{barcode}")
    public BarcodeProductDto lookupBarcode(@PathParam("barcode") String barcode) {
        return barcodeService.lookup(barcode);
    }
}
