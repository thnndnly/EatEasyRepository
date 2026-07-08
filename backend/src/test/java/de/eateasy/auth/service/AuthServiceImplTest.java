package de.eateasy.auth.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.LoginRequest;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.dto.UserDto;
import de.eateasy.auth.entity.User;
import de.eateasy.auth.google.GoogleIdTokenPayload;
import de.eateasy.auth.google.GoogleTokenVerifier;
import de.eateasy.auth.repository.UserRepository;
import de.eateasy.common.exception.EmailAlreadyExistsException;
import de.eateasy.common.exception.InvalidCredentialsException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class AuthServiceImplTest {

    @Inject
    AuthService authService;

    @Inject
    UserRepository userRepository;

    @InjectMock
    GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    @Transactional
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @TestTransaction
    @DisplayName("register legt User an und liefert ein JWT")
    void registerHappyPath() {
        AuthResponse response = authService.register(
            new RegisterRequest("Alice@example.com", "secret12", "Alice"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("alice@example.com");
        assertThat(response.user().displayName()).isEqualTo("Alice");
        assertThat(response.user().id()).isNotNull();
        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
    }

    @Test
    @TestTransaction
    @DisplayName("register wirft EmailAlreadyExistsException bei doppelter Email")
    void registerDuplicateEmail() {
        authService.register(new RegisterRequest("alice@example.com", "secret12", "Alice"));

        assertThatThrownBy(() ->
            authService.register(new RegisterRequest("ALICE@example.com", "secret34", "Alice 2")))
            .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("login mit korrektem Passwort liefert JWT")
    void loginHappyPath() {
        authService.register(new RegisterRequest("alice@example.com", "secret12", "Alice"));

        AuthResponse response = authService.login(new LoginRequest("alice@example.com", "secret12"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("alice@example.com");
    }

    @Test
    @TestTransaction
    @DisplayName("login mit falschem Passwort wirft InvalidCredentialsException")
    void loginWrongPassword() {
        authService.register(new RegisterRequest("alice@example.com", "secret12", "Alice"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("login mit unbekannter Email wirft InvalidCredentialsException")
    void loginUnknownEmail() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "secret12")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("loginWithGoogle legt fuer neue Email einen passwortlosen Google-User an")
    void googleLoginCreatesNewUser() {
        when(googleTokenVerifier.verify(anyString())).thenReturn(
            new GoogleIdTokenPayload("bob@example.com", true, "google-sub-1", "Bob G"));

        AuthResponse response = authService.loginWithGoogle("fake-id-token");

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("bob@example.com");
        assertThat(response.user().displayName()).isEqualTo("Bob G");
        User stored = userRepository.findByEmail("bob@example.com").orElseThrow();
        assertThat(stored.getPasswordHash()).isNull();
        assertThat(stored.getGoogleSub()).isEqualTo("google-sub-1");
    }

    @Test
    @TestTransaction
    @DisplayName("loginWithGoogle verknuepft bestehenden Passwort-Account per Email (kein Duplikat)")
    void googleLoginLinksExistingAccount() {
        authService.register(new RegisterRequest("alice@example.com", "secret12", "Alice"));
        when(googleTokenVerifier.verify(anyString())).thenReturn(
            new GoogleIdTokenPayload("ALICE@example.com", true, "google-sub-2", "Alice Google"));

        AuthResponse response = authService.loginWithGoogle("fake-id-token");

        assertThat(response.user().email()).isEqualTo("alice@example.com");
        // Kein zweiter Account, googleSub wurde am bestehenden gesetzt.
        assertThat(userRepository.count("email", "alice@example.com")).isEqualTo(1);
        User stored = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(stored.getGoogleSub()).isEqualTo("google-sub-2");
        assertThat(stored.getPasswordHash()).isNotNull(); // Passwort bleibt erhalten
    }

    @Test
    @TestTransaction
    @DisplayName("loginWithGoogle mit unverifizierter Email wirft InvalidCredentialsException")
    void googleLoginRejectsUnverifiedEmail() {
        when(googleTokenVerifier.verify(anyString())).thenReturn(
            new GoogleIdTokenPayload("bob@example.com", false, "google-sub-3", "Bob"));

        assertThatThrownBy(() -> authService.loginWithGoogle("fake-id-token"))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("Passwort-Login auf reinen Google-User (kein Passwort) wirft InvalidCredentialsException")
    void passwordLoginFailsForGoogleOnlyUser() {
        when(googleTokenVerifier.verify(anyString())).thenReturn(
            new GoogleIdTokenPayload("bob@example.com", true, "google-sub-4", "Bob"));
        authService.loginWithGoogle("fake-id-token"); // legt passwortlosen User an

        assertThatThrownBy(() -> authService.login(new LoginRequest("bob@example.com", "irgendwas")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("findByEmail liefert Optional.empty fuer unbekannte Email")
    void findByEmailReturnsEmptyForUnknown() {
        Optional<UserDto> result = authService.findByEmail("ghost@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("findByEmail liefert Optional.empty fuer null/leeren Input")
    void findByEmailReturnsEmptyForBlankInput() {
        assertThat(authService.findByEmail(null)).isEmpty();
        assertThat(authService.findByEmail("")).isEmpty();
        assertThat(authService.findByEmail("   ")).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("findByEmail findet User case-insensitive")
    void findByEmailCaseInsensitive() {
        authService.register(new RegisterRequest("alice@example.com", "secret12", "Alice"));

        Optional<UserDto> result = authService.findByEmail("ALICE@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("alice@example.com");
    }
}
