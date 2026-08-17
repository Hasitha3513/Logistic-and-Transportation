package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "driver")
@Getter
@Setter
@NoArgsConstructor
class DriverEntity {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "employee_number")
    private String employeeNumber;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "phone")
    private String phone;
    @Column(name = "email")
    private String email;
    @Column(name = "status")
    private String status;
    @Column(name = "active")
    private boolean active;

    public DriverEntity(UUID id, String employeeNumber, String firstName, String lastName, String phone, String email, String status, boolean active) {
        this.id = id;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.active = active;
    }
}
