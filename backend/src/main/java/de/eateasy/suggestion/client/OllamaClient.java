package de.eateasy.suggestion.client;

/**
 * Schmales Interface für den Aufruf gegen Ollama. Die Default-Implementierung
 * nutzt {@link java.net.http.HttpClient} direkt — das bringt deterministischere
 * URL-Auflösung als die {@code @RegisterRestClient}-Variante, die in unserem
 * Dev-Setup gegen den Quarkus-eigenen Port zurückfiel.
 */
public interface OllamaClient {

    OllamaGenerateResponse generate(OllamaGenerateRequest request);
}
