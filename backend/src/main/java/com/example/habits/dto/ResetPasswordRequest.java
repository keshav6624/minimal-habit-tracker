package com.example.habits.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "must not be blank")
        String token,

        @NotBlank(message = "must not be blank")
        @Size(min = 10, max = 100,
                message = "must be 10-100 characters with upper case, lower case and a digit")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "must contain upper case, lower case and a digit")
        String newPassword) {}
