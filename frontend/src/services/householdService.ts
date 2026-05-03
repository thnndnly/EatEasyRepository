import { apiFetch } from './apiClient'
import type {
  AcceptInvitationRequest,
  HouseholdCreateRequest,
  HouseholdDto,
  HouseholdUpdateRequest,
  InvitationCreateRequest,
  InvitationDto,
  MemberDto,
} from '@/types/household'

const BASE = '/api/v1'

export function listHouseholds(token: string): Promise<HouseholdDto[]> {
  return apiFetch<HouseholdDto[]>(`${BASE}/households`, { token })
}

export function getHousehold(token: string, id: string): Promise<HouseholdDto> {
  return apiFetch<HouseholdDto>(`${BASE}/households/${id}`, { token })
}

export function createHousehold(
  token: string,
  request: HouseholdCreateRequest,
): Promise<HouseholdDto> {
  return apiFetch<HouseholdDto>(`${BASE}/households`, {
    method: 'POST',
    body: request,
    token,
  })
}

export function updateHousehold(
  token: string,
  id: string,
  request: HouseholdUpdateRequest,
): Promise<HouseholdDto> {
  return apiFetch<HouseholdDto>(`${BASE}/households/${id}`, {
    method: 'PATCH',
    body: request,
    token,
  })
}

export function listMembers(token: string, householdId: string): Promise<MemberDto[]> {
  return apiFetch<MemberDto[]>(`${BASE}/households/${householdId}/members`, { token })
}

export function inviteMember(
  token: string,
  householdId: string,
  request: InvitationCreateRequest,
): Promise<InvitationDto> {
  return apiFetch<InvitationDto>(`${BASE}/households/${householdId}/invitations`, {
    method: 'POST',
    body: request,
    token,
  })
}

export function removeMember(
  token: string,
  householdId: string,
  memberId: string,
): Promise<void> {
  return apiFetch<void>(`${BASE}/households/${householdId}/members/${memberId}`, {
    method: 'DELETE',
    token,
  })
}

export function acceptInvitation(
  token: string,
  request: AcceptInvitationRequest,
): Promise<HouseholdDto> {
  return apiFetch<HouseholdDto>(`${BASE}/invitations/accept`, {
    method: 'POST',
    body: request,
    token,
  })
}
