package com.example.habits.dto;

import java.util.List;

/** Page envelope (no full Spring Page metadata is exposed). */
public record PageEnvelope<T>(List<T> content, int page, int size,
                              long totalElements, int totalPages) {}
