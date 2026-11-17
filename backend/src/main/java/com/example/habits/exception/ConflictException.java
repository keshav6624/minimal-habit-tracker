package com.example.habits.exception;

/** 409 — state conflict, e.g. duplicate email, PUT on archived habit. */
public class ConflictException extends ApiException {

    public ConflictException(ErrorType type, String detail) {
        super(type, 409, detail, null);
    }
}
