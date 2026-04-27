export interface HealthStatus {
  status: string
}

export async function fetchHealth(): Promise<HealthStatus> {
  const response = await fetch('/api/v1/health', {
    headers: { Accept: 'application/json' },
  })

  if (!response.ok) {
    throw new Error(`Backend health check failed: HTTP ${response.status}`)
  }

  return (await response.json()) as HealthStatus
}
