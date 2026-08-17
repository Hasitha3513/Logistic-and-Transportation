package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "fuel_limit_policy")
@Getter
@Setter
@NoArgsConstructor
class FuelLimitPolicyEntity {
    @Id
    private UUID id;
    @Column(name = "vehicle_id")
    private UUID vehicleId;
    @Column(name = "maximum_quantity_per_issue", nullable = false, precision = 19, scale = 3)
    private BigDecimal maximumQuantityPerIssue;
    @Column(nullable = false)
    private boolean active;
}
