import { apiFetch } from './apiClient'

export interface HealthStatus {
  status: string
}

export function fetchHealth(): Promise<HealthStatus> {
  return apiFetch<HealthStatus>('/api/v1/health')
}
