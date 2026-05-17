import { useAuthStore } from '@/stores/authStore'

/**
 * Liefert eine Funktion, die das aktuelle Auth-Token zurueckgibt oder einen
 * Fehler wirft, wenn niemand eingeloggt ist. Wird in Pinia-Stores benutzt,
 * um die Token-Pruefung nicht in jedem Action-Method zu wiederholen.
 */
export function useRequireToken(): () => string {
  const auth = useAuthStore()
  return () => {
    if (!auth.token) {
      throw new Error('Nicht eingeloggt')
    }
    return auth.token
  }
}
