package com.transportlogistics.app.identity.application.ports.out;

public interface PasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
