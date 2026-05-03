package de.eateasy.household.dto;

import de.eateasy.household.entity.MembershipRole;

import java.time.Instant;
import java.util.UUID;

public record MemberDto(
    UUID userId,
    String email,
    String displayName,
    MembershipRole role,
    Instant joinedAt
) {
}
