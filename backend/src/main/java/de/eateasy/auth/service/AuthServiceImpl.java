package de.eateasy.auth.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.LoginRequest;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.dto.UserDto;
import de.eateasy.auth.entity.User;
import de.eateasy.auth.repository.UserRepository;
import de.eateasy.common.exception.EmailAlreadyExistsException;
import de.eateasy.common.exception.InvalidCredentialsException;
import de.eateasy.common.exception.NotFoundException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Duration;
import java.util.UUID;

@ApplicationScoped
public class AuthServiceImpl implements AuthService {

    private static final String JWT_GROUP = "user";
    private static final String CLAIM_USER_ID = "uid";
    private static final Duration TOKEN_TTL = Duration.ofHours(8);
    private static final int BCRYPT_LOG_ROUNDS = 12;

    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
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
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
            .orElseThrow(InvalidCredentialsException::new);

        if (!BCrypt.checkpw(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new AuthResponse(issueToken(user), UserDto.from(user));
    }

    @Override
    public UserDto getCurrentUser(UUID userId) {
        return userRepository.findByIdOptional(userId)
            .map(UserDto::from)
            .orElseThrow(() -> new NotFoundException("User nicht gefunden: " + userId));
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
