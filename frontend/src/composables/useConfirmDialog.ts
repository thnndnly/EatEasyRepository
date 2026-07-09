/**
 * Async-Wrapper um `window.confirm`. Aktuell delegiert die Funktion direkt
 * an das Browser-Dialog — durch den async-Vertrag kann später ein eigenes
 * Modal-Dialog-UI eingebaut werden, ohne dass die Call-Sites geändert
 * werden müssen.
 */
export function useConfirmDialog(): (message: string) => Promise<boolean> {
  return async (message: string): Promise<boolean> => {
    if (typeof window === 'undefined') {
      return true
    }
    return window.confirm(message)
  }
}
