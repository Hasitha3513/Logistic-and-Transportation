package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryRiderTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID driverId = UUID.randomUUID();
    private final UUID primaryZone = UUID.randomUUID();
    private final UUID secondaryZone1 = UUID.randomUUID();
    private final UUID secondaryZone2 = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    @DisplayName("Should create valid DeliveryRider and manage secondary zones without duplicate primary")
    void createDeliveryRider_valid_succeeds() {
        DeliveryRider rider = DeliveryRider.create(
                UUID.randomUUID(),
                tenantId,
                "RDR-001",
                driverId,
                DeliveryRiderType.FULL_TIME,
                primaryZone,
                Set.of(primaryZone, secondaryZone1, secondaryZone2),
                10,
                "dispatcher",
                now
        );

        assertThat(rider.getRiderCode()).isEqualTo("RDR-001");
        assertThat(rider.getStatus()).isEqualTo(DeliveryRiderStatus.ACTIVE);
        assertThat(rider.getPrimaryZoneId()).isEqualTo(primaryZone);
        assertThat(rider.getSecondaryZoneIds()).containsExactlyInAnyOrder(secondaryZone1, secondaryZone2);
        assertThat(rider.getMaxConcurrentDeliveries()).isEqualTo(10);
        assertThat(rider.isEligibleForZone(primaryZone)).isTrue();
        assertThat(rider.isEligibleForZone(secondaryZone1)).isTrue();
        assertThat(rider.isEligibleForZone(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("Should validate DeliveryRider mandatory fields and capacity")
    void createDeliveryRider_invalid_throws() {
        assertThatThrownBy(() -> DeliveryRider.create(null, tenantId, "", driverId, DeliveryRiderType.GIG, primaryZone, null, 5, "actor", now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Rider code is required");

        assertThatThrownBy(() -> DeliveryRider.create(null, tenantId, "RDR-002", null, DeliveryRiderType.GIG, primaryZone, null, 5, "actor", now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Driver reference is required");

        assertThatThrownBy(() -> DeliveryRider.create(null, tenantId, "RDR-002", driverId, DeliveryRiderType.GIG, null, null, 5, "actor", now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Primary zone is required");

        assertThatThrownBy(() -> DeliveryRider.create(null, tenantId, "RDR-002", driverId, DeliveryRiderType.GIG, primaryZone, null, -1, "actor", now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Max concurrent deliveries must be positive");
    }

    @Test
    @DisplayName("Should handle status transitions correctly")
    void riderStatusTransitions() {
        DeliveryRider rider = DeliveryRider.create(
                UUID.randomUUID(), tenantId, "RDR-003", driverId, DeliveryRiderType.CONTRACTOR, primaryZone, null, 5, "actor", now
        );

        rider.deactivate("admin", now);
        assertThat(rider.getStatus()).isEqualTo(DeliveryRiderStatus.INACTIVE);

        rider.activate("admin", now);
        assertThat(rider.getStatus()).isEqualTo(DeliveryRiderStatus.ACTIVE);

        rider.suspend("admin", now);
        assertThat(rider.getStatus()).isEqualTo(DeliveryRiderStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Should detect shift overlap with half-open interval semantics")
    void shiftOverlapSemantics() {
        UUID riderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 1);

        DeliveryRiderShift shift = DeliveryRiderShift.create(
                UUID.randomUUID(), tenantId, riderId, date,
                LocalTime.of(9, 0), LocalTime.of(12, 0), null, 5, "actor", now
        );

        // Adjacent shift (12:00 to 15:00) does NOT overlap
        assertThat(shift.overlapsWith(date, LocalTime.of(12, 0), LocalTime.of(15, 0))).isFalse();

        // Adjacent shift (06:00 to 09:00) does NOT overlap
        assertThat(shift.overlapsWith(date, LocalTime.of(6, 0), LocalTime.of(9, 0))).isFalse();

        // Intersecting shifts overlap
        assertThat(shift.overlapsWith(date, LocalTime.of(10, 0), LocalTime.of(13, 0))).isTrue();
        assertThat(shift.overlapsWith(date, LocalTime.of(8, 0), LocalTime.of(10, 0))).isTrue();
        assertThat(shift.overlapsWith(date, LocalTime.of(9, 30), LocalTime.of(11, 30))).isTrue();

        // Different date does not overlap
        assertThat(shift.overlapsWith(date.plusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0))).isFalse();
    }

    @Test
    @DisplayName("Should validate shift state transitions")
    void shiftStateTransitions() {
        DeliveryRiderShift shift = DeliveryRiderShift.create(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), LocalDate.now(),
                LocalTime.of(8, 0), LocalTime.of(16, 0), null, 5, "actor", now
        );

        assertThat(shift.getStatus()).isEqualTo(DeliveryRiderShiftStatus.SCHEDULED);

        shift.startDuty("rider", now);
        assertThat(shift.getStatus()).isEqualTo(DeliveryRiderShiftStatus.ON_DUTY);

        shift.completeDuty("rider", now);
        assertThat(shift.getStatus()).isEqualTo(DeliveryRiderShiftStatus.COMPLETED);

        // Completed cannot be cancelled
        assertThatThrownBy(() -> shift.cancelShift("rider", now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Completed shift cannot be cancelled");
    }

    @Test
    @DisplayName("Should enforce override reason on assignment")
    void orderRiderAssignment_overrideReason() {
        UUID orderId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        assertThatThrownBy(() -> DeliveryOrderRiderAssignment.create(null, tenantId, orderId, riderId, true, " ", "admin", now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Override reason is mandatory");

        DeliveryOrderRiderAssignment assignment = DeliveryOrderRiderAssignment.create(null, tenantId, orderId, riderId, true, "Urgent VIP order", "admin", now);
        assertThat(assignment.isOverride()).isTrue();
        assertThat(assignment.getOverrideReason()).isEqualTo("Urgent VIP order");
        assertThat(assignment.getStatus()).isEqualTo(DeliveryRiderAssignmentStatus.ACTIVE);

        assignment.reassign("manager", now);
        assertThat(assignment.getStatus()).isEqualTo(DeliveryRiderAssignmentStatus.REASSIGNED);
    }
}
