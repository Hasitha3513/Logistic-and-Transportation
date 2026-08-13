package com.transportlogistics.app.identity.domain.model;

import java.util.UUID;

public record Role(UUID id, String name, String description, boolean active) {
}