package de.eateasy.auth.dto;

public record AuthResponse(String token, UserDto user) {
}
