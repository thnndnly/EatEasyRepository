package de.eateasy.suggestion.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Wählt zur Laufzeit die {@link OllamaClient}-Implementierung anhand von
 * {@code ai.provider}: {@code ollama} (Default, self-hosted lokal) oder
 * {@code groq} (gehostete OpenAI-kompatible API — z. B. für die Render-Demo,
 * wo kein Ollama läuft). Die konkreten Clients sind per {@code @Typed}
 * verborgen, damit hier keine Injection-Mehrdeutigkeit entsteht.
 */
@ApplicationScoped
public class LlmClientProducer {

    private static final Logger LOG = Logger.getLogger(LlmClientProducer.class);

    @Produces
    @ApplicationScoped
    public OllamaClient llmClient(
        @ConfigProperty(name = "ai.provider", defaultValue = "ollama") String provider,
        OllamaHttpClient ollamaClient,
        GroqClient groqClient
    ) {
        if ("groq".equalsIgnoreCase(provider.trim())) {
            LOG.info("LLM-Provider: groq (gehostete API)");
            return groqClient;
        }
        LOG.infof("LLM-Provider: ollama (self-hosted)");
        return ollamaClient;
    }
}
