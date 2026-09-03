package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fuel_issue")
@Getter
@Setter
@NoArgsConstructor
class FuelIssueEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id UUID id;
    @Column(name = "voucher_number", nullable = false, unique = true) String voucherNumber;
    @Column(name = "vehicle_id", nullable = false) UUID vehicleId;
    @Column(name = "trip_id") UUID tripId;
    @Column(name = "driver_id") UUID driverId;
    @Column(name = "fuel_type", nullable = false) String fuelType;
    @Column(nullable = false, precision = 19, scale = 3) BigDecimal quantity;
    @Column(name = "unit_price", precision = 19, scale = 4) BigDecimal unitPrice;
    @Column(name = "total_amount", precision = 19, scale = 2) BigDecimal totalAmount;
    @Column(name = "station_id", nullable = false) UUID stationId;
    @Column(precision = 19, scale = 3) BigDecimal odometer;
    @Column(name = "engine_hours", precision = 19, scale = 3) BigDecimal engineHours;
    @Column(name = "issue_date_time", nullable = false) OffsetDateTime issueDateTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false) FuelIssueStatus status;
    @Column(name = "requested_by", nullable = false) UUID requestedBy;
    @Column(name = "authorized_by") UUID authorizedBy;
    @Column(name = "authorization_date_time") OffsetDateTime authorizationDateTime;
    @Column(length = 1000) String notes;
    @Column(name = "created_at", nullable = false) OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) OffsetDateTime updatedAt;
}
