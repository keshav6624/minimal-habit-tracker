package com.example.habits.exception;

import java.util.List;

/**
 * Base class for all intentional API errors. Carries the error envelope fields;
 * {@link GlobalExceptionHandler} renders it as {@link ErrorResponse}.
 */
public class ApiException extends RuntimeException {

    private final ErrorType type;
    private final int status;
    private final String title;
    private final List<ErrorResponse.FieldError> fieldErrors;

    protected ApiException(ErrorType type, int status, String detail,
                           List<ErrorResponse.FieldError> fieldErrors) {
        super(detail);
        this.type = type;
        this.status = status;
        this.title = type.getDefaultTitle();
        this.fieldErrors = fieldErrors;
    }

    public ErrorType getType() {
        return type;
    }

    public int getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public List<ErrorResponse.FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public static ApiException of(ErrorType type, int status, String detail) {
        return new ApiException(type, status, detail, null);
    }

    public static ApiException validation(String detail, List<ErrorResponse.FieldError> fieldErrors) {
        return new ApiException(ErrorType.VALIDATION_ERROR, 400, detail, fieldErrors);
    }

    public static ApiException validation(String field, String message) {
        return validation("One or more fields are invalid",
                List.of(new ErrorResponse.FieldError(field, message)));
    }
}
