package com.example.habits.dto;

import java.util.UUID;

/** Public user representation (never exposes password hashes). */
public record UserDto(UUID id, String email, String displayName, String timezone) {}
