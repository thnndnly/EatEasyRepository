/**
 * Async-Wrapper um `window.confirm`. Aktuell delegiert die Funktion direkt
 * an das Browser-Dialog — durch den async-Vertrag kann spaeter ein eigenes
 * Modal-Dialog-UI eingebaut werden, ohne dass die Call-Sites geaendert
 * werden muessen.
 */
export function useConfirmDialog(): (message: string) => Promise<boolean> {
  return async (message: string): Promise<boolean> => {
    if (typeof window === 'undefined') {
      return true
    }
    return window.confirm(message)
  }
}
