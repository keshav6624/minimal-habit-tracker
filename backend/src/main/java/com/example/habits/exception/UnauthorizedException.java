package com.example.habits.exception;

/** 401 — authentication required or credentials invalid. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(ErrorType type, String detail) {
        super(type, 401, detail, null);
    }
}
