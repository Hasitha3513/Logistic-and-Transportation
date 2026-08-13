package com.transportlogistics.app.identity.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record User(UUID id, String username, String email, String passwordHash, String firstName, String lastName,
                   String phone, boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
