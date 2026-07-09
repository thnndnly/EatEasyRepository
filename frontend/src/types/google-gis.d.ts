// Minimale Typen für die extern geladene Google Identity Services (GIS)
// Bibliothek (accounts.google.com/gsi/client). Nur die genutzten Aufrufe.
export {}

declare global {
  interface GoogleCredentialResponse {
    credential: string
  }

  interface GoogleIdConfiguration {
    client_id: string
    callback: (response: GoogleCredentialResponse) => void
  }

  interface GoogleButtonOptions {
    theme?: 'outline' | 'filled_blue' | 'filled_black'
    size?: 'small' | 'medium' | 'large'
    text?: 'signin_with' | 'signup_with' | 'continue_with' | 'signin'
    shape?: 'rectangular' | 'pill' | 'circle' | 'square'
    width?: number
  }

  interface Window {
    google?: {
      accounts: {
        id: {
          initialize(config: GoogleIdConfiguration): void
          renderButton(parent: HTMLElement, options: GoogleButtonOptions): void
        }
      }
    }
  }
}
