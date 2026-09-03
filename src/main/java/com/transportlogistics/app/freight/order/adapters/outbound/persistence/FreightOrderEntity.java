package com.transportlogistics.app.freight.order.adapters.outbound.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "freight_order")
@Getter @Setter @NoArgsConstructor
class FreightOrderEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id UUID id;
    @Column(name = "order_number", nullable = false, unique = true, length = 60) String orderNumber;
    @Column(name = "customer_id", nullable = false) UUID customerId;
    @Column(name = "origin_location_id", nullable = false) UUID originLocationId;
    @Column(name = "destination_location_id", nullable = false) UUID destinationLocationId;
    @Column(name = "requested_pickup_at", nullable = false) OffsetDateTime requestedPickupAt;
    @Column(name = "requested_delivery_at", nullable = false) OffsetDateTime requestedDeliveryAt;
    @Column(name = "service_level", nullable = false, length = 60) String serviceLevel;
    @Column(nullable = false, length = 40) String priority;
    @Column(name = "special_handling_instructions", length = 2000) String specialHandlingInstructions;
    @Version @Column(nullable = false) long version;
    @Column(name = "created_at", nullable = false) OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) OffsetDateTime updatedAt;
    @Column(name = "created_by", nullable = false, length = 128) String createdBy;
    @Column(name = "updated_by", nullable = false, length = 128) String updatedBy;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("lineOrder ASC")
    List<FreightOrderLineEntity> lines = new ArrayList<>();

    void replaceLines(List<FreightOrderLineEntity> replacements) {
        lines.clear();
        replacements.forEach(line -> { line.setOrder(this); lines.add(line); });
    }
}
