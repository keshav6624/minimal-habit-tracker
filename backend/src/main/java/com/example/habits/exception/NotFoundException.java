package com.example.habits.exception;

/** 404 — also used when a resource exists but belongs to another user, to avoid enumeration. */
public class NotFoundException extends ApiException {

    public NotFoundException(String detail) {
        super(ErrorType.NOT_FOUND, 404, detail, null);
    }
}
