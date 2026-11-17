package com.example.habits.dto;

import jakarta.validation.constraints.NotBlank;

/** Account-deletion confirmation; the password must be re-verified. */
public record DeleteAccountRequest(
        @NotBlank(message = "must not be blank")
        String password) {}
