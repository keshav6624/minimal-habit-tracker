package com.example.habits.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration request. Password policy (min 10 chars with upper + lower +
 * digit) is enforced by the regex so the client gets field-level errors.
 */
public record RegisterRequest(
        @NotBlank(message = "must not be blank")
        @Email(message = "must be a valid email")
        @Size(max = 254, message = "must be at most 254 characters")
        String email,

        @NotBlank(message = "must not be blank")
        @Size(min = 10, max = 100,
                message = "must be 10-100 characters with upper case, lower case and a digit")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "must contain upper case, lower case and a digit")
        String password,

        @Size(max = 80, message = "must be at most 80 characters")
        String displayName,

        @NotBlank(message = "must not be blank (IANA timezone, e.g. Asia/Kolkata)")
        String timezone) {}
