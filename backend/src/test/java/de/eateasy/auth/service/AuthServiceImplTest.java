package de.eateasy.auth.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.LoginRequest;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.repository.UserRepository;
import de.eateasy.common.exception.EmailAlreadyExistsException;
import de.eateasy.common.exception.InvalidCredentialsException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class AuthServiceImplTest {

    @Inject
    AuthService authService;

    @Inject
    UserRepository userRepository;

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
}
