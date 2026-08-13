package com.transportlogistics.app.organization.domain.model;

import java.util.UUID;

public record Project(UUID id, String code, String name, UUID departmentId, boolean active) {
}
