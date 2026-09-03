package com.transportlogistics.app.shared.domain;

public class DependencyUnavailableException extends RuntimeException {
    private final String code;
    public DependencyUnavailableException(String code, String message, Throwable cause) { super(message, cause); this.code = code; }
    public String code() { return code; }
}
