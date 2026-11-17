package com.example.habits.security;

import com.example.habits.exception.ErrorType;
import com.example.habits.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * Session-based security for a REST/SPA client.
 *
 * <p>Model: CSRF protection is ALWAYS on (cookie-based token + XOR encoding,
 * SPA pattern); clients fetch GET /api/v1/auth/csrf first and send the raw
 * cookie value back in X-XSRF-TOKEN on unsafe methods. Sessions (12h, HttpOnly,
 * SameSite=Lax cookie named SESSION) authenticate the user; 401s are JSON —
 * there is no login page and no request-cache redirect. Rate limiting is a
 * separate servlet filter (@Order(HIGHEST_PRECEDENCE+10)) that runs before this
 * chain.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Slim UserDetails backed by our User entity. */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> userRepository.findByEmailIgnoreCase(email)
                .map(u -> org.springframework.security.core.userdetails.User
                        .withUsername(u.getEmail())
                        .password(u.getPasswordHash())
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No account with that email"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12 per the security spec; BCrypt hashes stay compatible across strengths.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /** Persists the auto-authentication context set up by register/login services. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new HttpSessionSecurityContextRepository(),
                new RequestAttributeSecurityContextRepository());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ObjectMapper objectMapper,
                                           SecurityContextRepository securityContextRepository)
            throws Exception {

        SpaCsrfTokenRequestHandler csrfRequestHandler = new SpaCsrfTokenRequestHandler();

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfRequestHandler))
                .authorizeHttpRequests(auth -> auth
                        // Only these are public; everything else requires auth.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(objectMapper, request, response, 401,
                                        ErrorType.UNAUTHORIZED, "Authentication required"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(objectMapper, request, response, 403,
                                        ErrorType.FORBIDDEN, "Access denied")))
                // REST semantics: 401 JSON, never a redirect to a login page.
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                // Logout is handled by POST /api/v1/auth/logout (controller + service).
                .logout(logout -> logout.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /** Shared logout logic used by POST /api/v1/auth/logout and DELETE /api/v1/users/me. */
    public static void clearSession(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);
        logoutHandler.logout(request, response, null);
    }

    /** Security entrypoint that writes the same JSON error envelope. */
    static void writeError(ObjectMapper objectMapper, HttpServletRequest request,
                           HttpServletResponse response, int status, ErrorType type,
                           String detail) {
        try {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String traceId = String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
            objectMapper.writeValue(response.getWriter(),
                    new com.example.habits.exception.ErrorResponse(
                            type, type.getDefaultTitle(), status, detail, null, traceId));
        } catch (Exception ignored) {
            response.setStatus(status);
        }
    }
}
