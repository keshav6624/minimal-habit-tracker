package com.example.habits.controller;

import com.example.habits.dto.ForgotPasswordRequest;
import com.example.habits.dto.LoginRequest;
import com.example.habits.dto.RegisterRequest;
import com.example.habits.dto.ResetPasswordRequest;
import com.example.habits.dto.UserDto;
import com.example.habits.service.AuthService;
import com.example.habits.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. All POSTs here are public but CSRF-protected:
 * clients call GET /api/v1/auth/csrf first and echo the XSRF-TOKEN cookie in
 * the X-XSRF-TOKEN header.
 */
@Tag(name = "auth", description = "Registration, login, logout, password reset")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register and auto-login")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterRequest request,
                            HttpServletRequest servletRequest,
                            HttpServletResponse servletResponse) {
        return authService.register(request, servletRequest, servletResponse);
    }

    @Operation(summary = "Log in")
    @PostMapping("/login")
    public UserDto login(@Valid @RequestBody LoginRequest request,
                         HttpServletRequest servletRequest,
                         HttpServletResponse servletResponse) {
        return authService.login(request, servletRequest, servletResponse);
    }

    @Operation(summary = "Log out (invalidates the session)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest,
                                       HttpServletResponse servletResponse) {
        authService.logout(servletRequest, servletResponse);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request a password reset (always 202, no enumeration)")
    @PostMapping("/forgot-password")
    public ResponseEntity<GenericAccepted> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted()
                .body(new GenericAccepted(
                        "If that email belongs to an account, a password reset link has been sent."));
    }

    /** Generic, enumeration-proof response body for forgot-password. */
    public record GenericAccepted(String detail) {}

    @Operation(summary = "Reset the password with a single-use token")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Touches the deferred CsrfToken so the XSRF-TOKEN cookie is written;
     * clients use this before their first unsafe request.
     */
    @Operation(summary = "Issue the CSRF cookie")
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken(); // materializes the deferred token -> cookie set
        return ResponseEntity.noContent().build();
    }
}
