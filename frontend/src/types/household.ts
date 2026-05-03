import type { DietTag } from './dietTags'

export type MembershipRole = 'OWNER' | 'MEMBER'

export interface HouseholdDto {
  id: string
  name: string
  defaultDietTags: DietTag[]
  role: MembershipRole
  createdAt: string
}

export interface MemberDto {
  userId: string
  email: string
  displayName: string
  role: MembershipRole
  joinedAt: string
}

export interface InvitationDto {
  id: string
  householdId: string
  householdName: string
  email: string
  token: string
  expiresAt: string
  createdAt: string
}

export interface HouseholdCreateRequest {
  name: string
  defaultDietTags?: DietTag[]
}

export interface HouseholdUpdateRequest {
  name?: string
  defaultDietTags?: DietTag[]
}

export interface InvitationCreateRequest {
  email: string
}

export interface AcceptInvitationRequest {
  token: string
}
