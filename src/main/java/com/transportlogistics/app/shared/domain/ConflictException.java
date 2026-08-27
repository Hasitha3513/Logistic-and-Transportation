package com.transportlogistics.app.shared.domain;

public class ConflictException extends RuntimeException {
    private final String code;

    public ConflictException(String message) {
        this("ALLOCATION_CONFLICT", message);
    }

    public ConflictException(String message, Throwable cause) {
        this("ALLOCATION_CONFLICT", message, cause);
    }

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ConflictException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
