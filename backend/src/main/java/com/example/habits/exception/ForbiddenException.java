package com.example.habits.exception;

/** 403 — authenticated but not allowed (rare: ownership gaps return 404 instead). */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String detail) {
        super(ErrorType.FORBIDDEN, 403, detail, null);
    }
}
