package de.eateasy.suggestion.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client gegen Ollama (Self-Hosted LLM). Base-URL via
 * {@code quarkus.rest-client.ollama.url}. Timeouts greifen ueber
 * {@code quarkus.rest-client.ollama.connect-timeout/read-timeout}, damit
 * ein langsam laufendes Modell die Suggestion-Pipeline nicht endlos
 * blockiert — bei Fehler/Timeout greift der Coverage-Fallback im Service.
 */
@RegisterRestClient(configKey = "ollama")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OllamaClient {

    @POST
    @Path("/api/generate")
    OllamaGenerateResponse generate(OllamaGenerateRequest request);
}
