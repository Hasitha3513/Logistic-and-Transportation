package com.transportlogistics.app.fleet.domain.model;

import java.util.UUID;

public record Driver(UUID id, String employeeNumber, String firstName, String lastName, String phone, String email,
                     String status, boolean active) {
}
