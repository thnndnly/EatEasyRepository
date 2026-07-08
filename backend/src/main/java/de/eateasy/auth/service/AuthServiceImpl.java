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
import de.eateasy.common.exception.NotFoundException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuthServiceImpl implements AuthService {

    private static final String JWT_GROUP = "user";
    private static final String CLAIM_USER_ID = "uid";
    private static final Duration TOKEN_TTL = Duration.ofHours(8);
    private static final int BCRYPT_LOG_ROUNDS = 12;

    private final UserRepository userRepository;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthServiceImpl(UserRepository userRepository,
                           GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        String passwordHash = BCrypt.hashpw(request.password(), BCrypt.gensalt(BCRYPT_LOG_ROUNDS));
        User user = new User(email, passwordHash, request.displayName().trim());
        userRepository.persist(user);

        return new AuthResponse(issueToken(user), UserDto.from(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
            .orElseThrow(InvalidCredentialsException::new);

        // Reine Google-User haben kein Passwort → Passwort-Login schlaegt fehl.
        if (user.getPasswordHash() == null
            || !BCrypt.checkpw(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new AuthResponse(issueToken(user), UserDto.from(user));
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleIdTokenPayload payload = googleTokenVerifier.verify(idToken);
        if (payload.email() == null || !payload.emailVerified()) {
            // Ohne verifizierte Email koennen wir nicht per Email verknuepfen.
            throw new InvalidCredentialsException();
        }
        String email = normalizeEmail(payload.email());

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            String displayName = payload.name() != null && !payload.name().isBlank()
                ? payload.name().trim()
                : email;
            user = User.forGoogle(email, displayName, payload.sub());
            userRepository.persist(user);
        } else if (user.getGoogleSub() == null) {
            // Bestehenden (z. B. Passwort-)Account mit Google verknuepfen.
            user.setGoogleSub(payload.sub());
        }

        return new AuthResponse(issueToken(user), UserDto.from(user));
    }

    @Override
    public UserDto getUser(UUID userId) {
        return userRepository.findByIdOptional(userId)
            .map(UserDto::from)
            .orElseThrow(() -> new NotFoundException("User nicht gefunden: " + userId));
    }

    @Override
    public Optional<UserDto> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(normalizeEmail(email))
            .map(UserDto::from);
    }

    @Override
    public Map<UUID, UserDto> getUsers(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findByIds(userIds).stream()
            .map(UserDto::from)
            .collect(Collectors.toMap(UserDto::id, Function.identity()));
    }

    private String issueToken(User user) {
        return Jwt.upn(user.getEmail())
            .groups(JWT_GROUP)
            .claim(CLAIM_USER_ID, user.getId().toString())
            .expiresIn(TOKEN_TTL)
            .sign();
    }

    private static String normalizeEmail(String raw) {
        return raw.trim().toLowerCase();
    }
}
