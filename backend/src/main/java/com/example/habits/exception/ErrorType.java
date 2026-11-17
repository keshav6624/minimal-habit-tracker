package com.example.habits.exception;

/**
 * Machine-readable error types used in the {@code type} field of the error
 * envelope. One-to-one with the API contract.
 */
public enum ErrorType {
    VALIDATION_ERROR("Request validation failed"),
    UNAUTHORIZED("Authentication required"),
    INVALID_CREDENTIALS("Invalid credentials"),
    FORBIDDEN("Access denied"),
    NOT_FOUND("Resource not found"),
    CONFLICT("Request conflicts with current state"),
    EMAIL_ALREADY_EXISTS("Email already registered"),
    RATE_LIMITED("Too many requests"),
    INTERNAL_ERROR("Internal server error");
    // NOTE: keep in sync with docs/API_SPEC.md if extended.

    private final String defaultTitle;

    ErrorType(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }
}
