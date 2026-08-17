package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fuel_issue")
@Getter
@Setter
@NoArgsConstructor
class FuelIssueEntity {
    @Id
    private UUID id;
    @Column(name = "voucher_number", nullable = false, unique = true)
    private String voucherNumber;
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;
    @Column(name = "trip_id")
    private UUID tripId;
    @Column(name = "driver_id")
    private UUID driverId;
    @Column(name = "fuel_type", nullable = false)
    private String fuelType;
    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;
    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;
    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "station_id", nullable = false)
    private UUID stationId;
    @Column(precision = 19, scale = 3)
    private BigDecimal odometer;
    @Column(name = "engine_hours", precision = 19, scale = 3)
    private BigDecimal engineHours;
    @Column(name = "issue_date_time", nullable = false)
    private OffsetDateTime issueDateTime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuelIssueStatus status;
    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;
    @Column(name = "authorized_by")
    private UUID authorizedBy;
    @Column(name = "authorization_date_time")
    private OffsetDateTime authorizationDateTime;
    @Column(length = 1000)
    private String notes;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
