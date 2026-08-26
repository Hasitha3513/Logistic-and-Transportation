package com.transportlogistics.app.fleet.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DriverViolationTest {

    @Test
    void recordsValidViolationWithDefaults() {
        var driverId = UUID.randomUUID();
        var violation = DriverViolation.record(
                driverId,
                null,
                DriverViolationType.SPEEDING,
                ViolationSeverity.MODERATE,
                OffsetDateTime.now().minusDays(1),
                3,
                new BigDecimal("150.00"),
                "Route 66",
                "Exceeded speed limit by 20km/h",
                "admin"
        );

        assertNotNull(violation.id());
        assertEquals(driverId, violation.driverId());
        assertEquals(DriverViolationType.SPEEDING, violation.violationType());
        assertEquals(ViolationSeverity.MODERATE, violation.severity());
        assertEquals(3, violation.penaltyPoints());
        assertEquals(new BigDecimal("150.00"), violation.fineAmount());
        assertEquals(FinePaymentStatus.UNPAID, violation.paymentStatus());
        assertNull(violation.paidAt());
        assertNull(violation.paymentReference());
    }

    @Test
    void rejectsNegativePenaltyPointsOrFines() {
        var driverId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        assertThrows(BusinessRuleException.class, () -> new DriverViolation(
                UUID.randomUUID(), driverId, null, DriverViolationType.SPEEDING, ViolationSeverity.MINOR,
                now, -1, new BigDecimal("50.00"), FinePaymentStatus.UNPAID, null, null, null, null, now, now, "admin", "admin"
        ));

        assertThrows(BusinessRuleException.class, () -> new DriverViolation(
                UUID.randomUUID(), driverId, null, DriverViolationType.SPEEDING, ViolationSeverity.MINOR,
                now, 0, new BigDecimal("-10.00"), FinePaymentStatus.UNPAID, null, null, null, null, now, now, "admin", "admin"
        ));
    }

    @Test
    void paysFineSuccessfully() {
        var violation = DriverViolation.record(
                UUID.randomUUID(),
                null,
                DriverViolationType.RED_LIGHT,
                ViolationSeverity.MAJOR,
                OffsetDateTime.now().minusDays(2),
                4,
                new BigDecimal("250.00"),
                "Intersection 5th & Main",
                "Passed red signal",
                "officer"
        );

        var paidAt = OffsetDateTime.now();
        var paid = violation.pay(paidAt, "RECEIPT-9988", "accountant");

        assertEquals(FinePaymentStatus.PAID, paid.paymentStatus());
        assertEquals(paidAt, paid.paidAt());
        assertEquals("RECEIPT-9988", paid.paymentReference());
        assertEquals("accountant", paid.updatedBy());

        // Re-paying or paying waived throws
        assertThrows(BusinessRuleException.class, () -> paid.pay(paidAt, "AGAIN", "accountant"));
    }

    @Test
    void waivesAndDisputesFine() {
        var violation = DriverViolation.record(
                UUID.randomUUID(),
                null,
                DriverViolationType.UNAUTHORIZED_STOP,
                ViolationSeverity.MINOR,
                OffsetDateTime.now().minusDays(1),
                1,
                new BigDecimal("50.00"),
                "Rest area",
                "Stopped outside schedule",
                "officer"
        );

        var waived = violation.waive("First warning waiver", "fleet_manager");
        assertEquals(FinePaymentStatus.WAIVED, waived.paymentStatus());
        assertTrue(waived.description().contains("[WAIVED: First warning waiver]"));

        assertThrows(BusinessRuleException.class, () -> waived.pay(OffsetDateTime.now(), "REF", "user"));
        assertThrows(BusinessRuleException.class, () -> waived.waive("Again", "user"));

        var disputeCandidate = DriverViolation.record(
                UUID.randomUUID(),
                null,
                DriverViolationType.OTHER,
                ViolationSeverity.MINOR,
                OffsetDateTime.now().minusDays(1),
                0,
                new BigDecimal("20.00"),
                "Depot",
                "Parking issue",
                "officer"
        );
        var disputed = disputeCandidate.dispute("Assigned spot was blocked", "driver");
        assertEquals(FinePaymentStatus.DISPUTED, disputed.paymentStatus());
        assertTrue(disputed.description().contains("[DISPUTED: Assigned spot was blocked]"));
    }
}
