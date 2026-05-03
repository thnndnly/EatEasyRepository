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

    /**
     * Lookup eines Users per ID. Wird von anderen Komponenten genutzt, die den
     * User nur als Foreign-Key kennen (z. B. Household-Mitglieder). Wirft
     * {@link de.eateasy.common.exception.NotFoundException} wenn der User nicht
     * existiert.
     */
    UserDto getUser(UUID userId);

    /**
     * Sucht einen User per Email (case-insensitive). Liefert {@code null}, wenn
     * kein User mit der Email existiert. Wird z. B. fuer Invitation-Annahme
     * genutzt, wo nicht jeder Empfaenger schon registriert sein muss.
     */
    UserDto findByEmail(String email);
}
