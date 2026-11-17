package com.example.habits.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * The one and only error envelope. Never contains stack traces, SQL messages,
 * or other internals.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        ErrorType type,
        String title,
        int status,
        String detail,
        List<FieldError> fieldErrors,
        String traceId) {

    /** One invalid field, e.g. field="name", message="must not be blank". */
    public record FieldError(String field, String message) {}
}
