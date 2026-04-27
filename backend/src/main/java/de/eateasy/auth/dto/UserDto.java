package de.eateasy.auth.dto;

import de.eateasy.auth.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
    UUID id,
    String email,
    String displayName,
    Instant createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
