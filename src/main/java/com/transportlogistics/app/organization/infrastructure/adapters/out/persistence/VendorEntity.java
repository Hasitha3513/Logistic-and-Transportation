package com.transportlogistics.app.organization.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "vendor")
@Getter
@Setter
@NoArgsConstructor
class VendorEntity {
    @Id UUID id;
    @Column(nullable = false, unique = true, length = 40) String code;
    @Column(nullable = false, length = 160) String name;
    @Column(name = "contact_person", length = 160) String contactPerson;
    @Column(length = 40) String phone;
    @Column(length = 160) String email;
    @Column(nullable = false) boolean active;
}
