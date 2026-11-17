package com.example.habits.service;

import com.example.habits.domain.PasswordResetToken;
import com.example.habits.domain.User;
import com.example.habits.domain.UserSettings;
import com.example.habits.dto.ForgotPasswordRequest;
import com.example.habits.dto.LoginRequest;
import com.example.habits.dto.RegisterRequest;
import com.example.habits.dto.ResetPasswordRequest;
import com.example.habits.dto.UserDto;
import com.example.habits.exception.ApiException;
import com.example.habits.exception.ConflictException;
import com.example.habits.exception.ErrorType;
import com.example.habits.exception.UnauthorizedException;
import com.example.habits.repository.PasswordResetTokenRepository;
import com.example.habits.repository.UserRepository;
import com.example.habits.repository.UserSettingsRepository;
import com.example.habits.security.SecurityConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Registration, login, logout and password-reset flows.
 *
 * <p>Enumeration defences: forgot-password always answers 202 with a generic
 * detail whether or not the email exists; login returns the same 401 shape for
 * unknown email and wrong password. Reset tokens are stored hashed (SHA-256);
 * the raw token is only delivered via the dev-log when
 * {@code app.auth.dev-expose-reset-token=true} (no real email provider yet).</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CurrentUserService currentUserService;
    private final TimezoneService timezoneService;
    private final boolean devExposeResetToken;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       UserSettingsRepository userSettingsRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       CurrentUserService currentUserService,
                       TimezoneService timezoneService,
                       @Value("${app.auth.dev-expose-reset-token:false}") boolean devExposeResetToken) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.currentUserService = currentUserService;
        this.timezoneService = timezoneService;
        this.devExposeResetToken = devExposeResetToken;
    }

    @Transactional
    public UserDto register(RegisterRequest request,
                            HttpServletRequest servletRequest,
                            HttpServletResponse servletResponse) {
        timezoneService.validate(request.timezone());
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(ErrorType.EMAIL_ALREADY_EXISTS,
                    "An account with this email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                ? null : request.displayName().trim());
        user.setTimezone(request.timezone());
        userRepository.save(user);

        // Default settings row: week_starts_on=1 (Monday), reminders off.
        UserSettings settings = new UserSettings(user);
        userSettingsRepository.save(settings);

        // Auto-authenticate the new user in the same response.
        currentUserService.signIn(user, servletRequest, servletResponse);
        return CurrentUserService.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto login(LoginRequest request,
                         HttpServletRequest servletRequest,
                         HttpServletResponse servletResponse) {
        try {
            // DaoAuthenticationProvider maps unknown email AND wrong password to
            // the same BadCredentialsException, so both produce identical 401s.
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    normalizeEmail(request.email()), request.password()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException(ErrorType.INVALID_CREDENTIALS,
                    "Invalid email or password");
        }
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException(ErrorType.INVALID_CREDENTIALS,
                        "Invalid email or password"));
        currentUserService.signIn(user, servletRequest, servletResponse);
        return CurrentUserService.toDto(user);
    }

    /** Invalidates the session and clears the security context; 204. */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityConfig.clearSession(request, response);
        SecurityContextHolder.clearContext();
    }

    /** ALWAYS 202 with a generic detail — never reveals whether the email exists. */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = generateRawToken();
            String hash = sha256Hex(rawToken);
            resetTokenRepository.save(new PasswordResetToken(
                    user, hash, Instant.now().plus(RESET_TOKEN_TTL)));
            // Dev-only delivery channel until a real mail provider exists.
            if (devExposeResetToken) {
                log.info("[DEV] Password reset token for {} : {}", email, rawToken);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository
                .findByTokenHash(sha256Hex(request.token()))
                .orElseThrow(() -> ApiException.validation(
                        "token", "is invalid, expired or already used"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.validation("token", "is invalid, expired or already used");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        resetTokenRepository.save(token);
        // Defence in depth: consume every other outstanding token for this user.
        resetTokenRepository.invalidateAllForUser(user.getId(), Instant.now());
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** 32 random bytes, URL-safe base64 — opaque but email/URL friendly. */
    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
