package de.eateasy.household.dto;

import de.eateasy.household.entity.HouseholdInvitation;

import java.time.Instant;
import java.util.UUID;

/**
 * Wird nach dem Erstellen einer Einladung zurueckgegeben. Solange E-Mail-
 * Versand nur simuliert ist (Phase 2), zeigt das Frontend den Token-Link
 * direkt an. In Phase 10 wird {@code token} server-seitig nicht mehr
 * exponiert.
 */
public record InvitationDto(
    UUID id,
    UUID householdId,
    String householdName,
    String email,
    String token,
    Instant expiresAt,
    Instant createdAt
) {
    public static InvitationDto from(HouseholdInvitation invitation, String householdName) {
        return new InvitationDto(
            invitation.getId(),
            invitation.getHouseholdId(),
            householdName,
            invitation.getEmail(),
            invitation.getToken(),
            invitation.getExpiresAt(),
            invitation.getCreatedAt());
    }
}
