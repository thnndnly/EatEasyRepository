package de.eateasy.auth.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.LoginRequest;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.dto.UserDto;

import java.util.UUID;

public interface AuthService {

    /** Legt einen neuen User an und gibt ein frisches JWT zurueck. */
    AuthResponse register(RegisterRequest request);

    /** Prueft Credentials und gibt ein frisches JWT zurueck. */
    AuthResponse login(LoginRequest request);

    /** Liefert den aktuell eingeloggten User anhand seiner ID. */
    UserDto getCurrentUser(UUID userId);
}
