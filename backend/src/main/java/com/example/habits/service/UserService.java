package com.example.habits.service;

import com.example.habits.domain.User;
import com.example.habits.domain.UserSettings;
import com.example.habits.dto.SettingsDto;
import com.example.habits.dto.UserDto;
import com.example.habits.exception.NotFoundException;
import com.example.habits.exception.UnauthorizedException;
import com.example.habits.exception.ErrorType;
import com.example.habits.repository.UserRepository;
import com.example.habits.repository.UserSettingsRepository;
import com.example.habits.security.SecurityConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Current-user profile, settings, and account deletion.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordVerifier passwordVerifier;
    private final TimezoneService timezoneService;

    public UserService(UserRepository userRepository,
                       UserSettingsRepository userSettingsRepository,
                       PasswordEncoderHolder holder,
                       TimezoneService timezoneService) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.passwordVerifier = holder;
        this.timezoneService = timezoneService;
    }

    public UserDto getMe(User user) {
        return CurrentUserService.toDto(user);
    }

    @Transactional
    public SettingsDto updateSettings(User detachedUser, SettingsDto request) {
        timezoneService.validate(request.timezone());
        // The session principal's User is detached — re-load a managed copy;
        // persisting UserSettings with a detached user association fails.
        User user = userRepository.findById(detachedUser.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        // users.timezone is the single source for date logic (completions,
        // analytics); a timezone change must propagate there immediately.
        user.setTimezone(request.timezone());
        UserSettings settings = userSettingsRepository.findById(user.getId())
                .orElseGet(() -> new UserSettings(user));
        settings.setUser(user);
        settings.setWeekStartsOn(request.weekStartsOn().shortValue());
        settings.setRemindersEnabled(request.remindersEnabled());
        userSettingsRepository.save(settings);
        // Return the saved state (the session principal's copy is stale).
        return new SettingsDto(user.getTimezone(),
                (int) settings.getWeekStartsOn(), settings.isRemindersEnabled());
    }

    @Transactional(readOnly = true)
    public SettingsDto getSettings(User user) {
        UserSettings settings = userSettingsRepository.findById(user.getId())
                .orElseGet(() -> {
                    UserSettings defaults = new UserSettings(user);
                    defaults.setWeekStartsOn((short) 1);
                    return defaults;
                });
        return new SettingsDto(user.getTimezone(),
                (int) settings.getWeekStartsOn(), settings.isRemindersEnabled());
    }

    /**
     * Hard-deletes the account after re-verifying the password. CASCADE FKs
     * remove habits, completions, settings, and reset tokens.
     */
    @Transactional
    public void deleteAccount(User user, String rawPassword,
                              HttpServletRequest request, HttpServletResponse response) {
        if (!passwordVerifier.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException(ErrorType.INVALID_CREDENTIALS,
                    "Invalid password");
        }
        userRepository.delete(user);
        SecurityConfig.clearSession(request, response);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    /** Tiny indirection so this class does not also own encoder wiring. */
    public interface PasswordVerifier {
        boolean matches(String raw, String hashed);
    }

    @org.springframework.stereotype.Component
    public static class PasswordEncoderHolder implements PasswordVerifier {

        private final org.springframework.security.crypto.password.PasswordEncoder encoder;

        public PasswordEncoderHolder(org.springframework.security.crypto.password.PasswordEncoder encoder) {
            this.encoder = encoder;
        }

        @Override
        public boolean matches(String raw, String hashed) {
            return encoder.matches(raw, hashed);
        }
    }
}
