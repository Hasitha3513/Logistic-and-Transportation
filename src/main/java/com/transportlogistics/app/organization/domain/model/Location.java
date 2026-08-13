package com.transportlogistics.app.organization.domain.model;

import java.util.UUID;

public record Location(UUID id, String code, String name, String address, Double latitude, Double longitude,
                       boolean active) {
}
