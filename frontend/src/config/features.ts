/**
 * Build-Zeit-Feature-Flags. Auf Render (kein Tesseract/Ollama im Free-Tier)
 * wird VITE_FEATURE_RECEIPT=false gesetzt — der Scanner-Button verschwindet
 * dann komplett aus dem UI, das Backend antwortet dort ohnehin mit 404.
 */
export const FEATURE_RECEIPT: boolean = import.meta.env.VITE_FEATURE_RECEIPT !== 'false'

/**
 * Google-OAuth-Client-ID (Web) aus der Google Cloud Console. Ist sie nicht
 * gesetzt, verschwindet der Google-Login-Button und das Backend antwortet auf
 * /auth/google mit 404 — das Feature ist dann schlicht aus.
 */
const rawGoogleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
export const GOOGLE_CLIENT_ID: string =
  typeof rawGoogleClientId === 'string' ? rawGoogleClientId.trim() : ''
export const FEATURE_GOOGLE_OAUTH: boolean = GOOGLE_CLIENT_ID.length > 0
