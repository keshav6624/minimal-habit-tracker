package com.example.habits.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record ForgotPasswordRequest(
        @NotBlank(message = "must not be blank")
        @Email(message = "must be a valid email")
        String email) {}
