package com.transportlogistics.app.identity.domain;

public final class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
