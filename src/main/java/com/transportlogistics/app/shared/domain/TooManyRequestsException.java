package com.transportlogistics.app.shared.domain;

public final class TooManyRequestsException extends RuntimeException {
    private final String code;
    public TooManyRequestsException(String code, String message) { super(message); this.code = code; }
    public String code() { return code; }
}
