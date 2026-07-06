package de.eateasy.receipt.client;

/**
 * Schmales Interface fuer die OCR-Erkennung. Die Default-Implementierung
 * spricht den Tesseract-HTTP-Wrapper aus docker-compose an; im Test wird
 * das Interface gemockt.
 */
public interface OcrClient {

    /**
     * Extrahiert Text aus einem Belegfoto.
     *
     * @param imageBytes Rohbytes des Bildes (JPEG/PNG)
     * @param filename   Original-Dateiname (nur fuer das Multipart-Feld)
     * @return erkannter Rohtext, ggf. leer — nie {@code null}
     */
    String extractText(byte[] imageBytes, String filename);
}
