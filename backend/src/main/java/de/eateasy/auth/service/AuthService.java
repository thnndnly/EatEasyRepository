package de.eateasy.auth.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.LoginRequest;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.dto.UserDto;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AuthService {

    /** Legt einen neuen User an und gibt ein frisches JWT zurück. */
    AuthResponse register(RegisterRequest request);

    /** Prüft Credentials und gibt ein frisches JWT zurück. */
    AuthResponse login(LoginRequest request);

    /**
     * Login/Registrierung per Google-ID-Token: verifiziert das Token, verknüpft
     * es per Email mit einem bestehenden Account oder legt einen neuen (passwort-
     * losen) Google-User an, und gibt ein frisches JWT zurück.
     */
    AuthResponse loginWithGoogle(String idToken);

    /**
     * Lookup eines Users per ID. Wird von anderen Komponenten genutzt, die den
     * User nur als Foreign-Key kennen (z. B. Household-Mitglieder). Wirft
     * {@link de.eateasy.common.exception.NotFoundException} wenn der User nicht
     * existiert.
     */
    UserDto getUser(UUID userId);

    /**
     * Sucht einen User per Email (case-insensitive). Liefert ein leeres
     * {@link Optional}, wenn kein User mit der Email existiert. Wird z. B. für
     * Invitation-Annahme genutzt, wo nicht jeder Empfänger schon registriert
     * sein muss.
     */
    Optional<UserDto> findByEmail(String email);

    /**
     * Batch-Lookup mehrerer User per ID. Wird von anderen Komponenten genutzt,
     * die mehrere Foreign-Keys auf einmal auflösen müssen (z. B.
     * {@code HouseholdService.listMembers}). Fehlende IDs tauchen schlicht
     * nicht in der Map auf — kein Throw.
     */
    Map<UUID, UserDto> getUsers(Collection<UUID> userIds);
}
