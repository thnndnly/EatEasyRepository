/**
 * Build-Zeit-Feature-Flags. Auf Render (kein Tesseract/Ollama im Free-Tier)
 * wird VITE_FEATURE_RECEIPT=false gesetzt — der Scanner-Button verschwindet
 * dann komplett aus dem UI, das Backend antwortet dort ohnehin mit 404.
 */
export const FEATURE_RECEIPT: boolean = import.meta.env.VITE_FEATURE_RECEIPT !== 'false'
