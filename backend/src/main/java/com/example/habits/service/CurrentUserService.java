package com.example.habits.service;

import com.example.habits.domain.User;
import com.example.habits.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Resolves the authenticated caller and establishes the security context after
 * register/login (auto-authentication).
 *
 * <p>The principal is always this app's {@link SecurityUser} wrapper of the
 * {@link User} entity — never the raw password holder.</p>
 */
@Service
public class CurrentUserService {

    private static final List<GrantedAuthority> USER_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final SecurityContextRepository securityContextRepository;

    public CurrentUserService(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    /** The authenticated caller; protected endpoints only. */
    public User requireUser() {
        User user = currentUserOrNull();
        if (user == null) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return user;
    }

    /** Null when unauthenticated (used by permitAll controllers). */
    public User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.user;
        }
        return null;
    }

    /**
     * Establishes an authenticated session (auto-login after register/login).
     * Rotates the session id first so a pre-login session id cannot be reused
     * (session fixation defence).
     */
    public void signIn(User user, HttpServletRequest request, HttpServletResponse response) {
        var existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
        request.getSession(true); // fresh session for the authenticated context

        SecurityUser principal = new SecurityUser(user);
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, USER_AUTHORITIES);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /** Principal stored in the security context; wraps the JPA entity. */
    public record SecurityUser(User user) implements Authentication {

        @Override
        public List<GrantedAuthority> getAuthorities() {
            return USER_AUTHORITIES;
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return user;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Authenticate via signIn()");
        }

        @Override
        public String getName() {
            return user.getEmail();
        }
    }

    public static UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getTimezone());
    }

    public static UUID userIdOf(User user) {
        return user.getId();
    }
}
