package com.example.habits.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "must not be blank")
        @Email(message = "must be a valid email")
        String email,

        @NotBlank(message = "must not be blank")
        String password) {}
