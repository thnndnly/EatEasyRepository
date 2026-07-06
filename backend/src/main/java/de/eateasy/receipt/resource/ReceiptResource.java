package de.eateasy.receipt.resource;

import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.exception.NotFoundException;
import de.eateasy.common.security.CurrentUser;
import de.eateasy.receipt.dto.ReceiptScanResponse;
import de.eateasy.receipt.service.ReceiptScanService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Beleg-Scanner (Phase 11, Stretch). Steht hinter dem Feature-Flag
 * {@code eateasy.receipt.enabled} — ist es aus, antwortet der Endpoint mit
 * 404, als gaebe es das Feature nicht (Demo-Deployments ohne
 * Tesseract/Ollama-Container).
 */
@Path("/api/v1/households/{householdId}/receipts")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@Tag(name = "Beleg-Scanner", description = "Kassenbon fotografieren, OCR + LLM-Extraktion, Vorschau fuer den Vorrat.")
@SecurityRequirement(name = "BearerAuth")
public class ReceiptResource {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private final ReceiptScanService receiptScanService;
    private final CurrentUser currentUser;
    private final boolean enabled;

    public ReceiptResource(ReceiptScanService receiptScanService,
                           CurrentUser currentUser,
                           @ConfigProperty(name = "eateasy.receipt.enabled", defaultValue = "true")
                           boolean enabled) {
        this.receiptScanService = receiptScanService;
        this.currentUser = currentUser;
        this.enabled = enabled;
    }

    @POST
    @Path("/scan")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
        summary = "Belegfoto scannen",
        description = "OCR via Tesseract + Strukturierung via Ollama. Persistiert nichts — "
            + "die Uebernahme in den Vorrat laeuft ueber die bestehende Pantry-API."
    )
    @APIResponse(responseCode = "200", description = "Rohtext + erkannte Posten (items kann leer sein).")
    @APIResponse(responseCode = "400", description = "Kein Bild, zu gross oder kein Text erkennbar.")
    @APIResponse(responseCode = "404", description = "Feature deaktiviert oder Haushalt unbekannt.")
    public ReceiptScanResponse scan(
        @Parameter(description = "Haushalt-UUID.") @PathParam("householdId") UUID householdId,
        @RestForm("file") FileUpload file
    ) {
        if (!enabled) {
            throw new NotFoundException("Beleg-Scanner ist nicht aktiviert");
        }
        if (file == null || file.uploadedFile() == null) {
            throw new BadRequestException("Es wurde keine Datei hochgeladen (Feld 'file')");
        }
        if (file.size() > MAX_FILE_BYTES) {
            throw new BadRequestException("Datei zu gross (max. 10 MB)");
        }

        byte[] imageBytes;
        try {
            imageBytes = Files.readAllBytes(file.uploadedFile());
        } catch (IOException ex) {
            throw new BadRequestException("Upload konnte nicht gelesen werden");
        }
        if (imageBytes.length == 0) {
            throw new BadRequestException("Hochgeladene Datei ist leer");
        }

        return receiptScanService.scan(
            currentUser.id(), householdId, imageBytes, file.fileName());
    }
}
