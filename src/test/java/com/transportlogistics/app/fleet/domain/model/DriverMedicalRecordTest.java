package com.transportlogistics.app.fleet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DriverMedicalRecordTest {

    @Test
    @DisplayName("Should create valid FIT medical record")
    void shouldCreateValidFitRecord() {
        var now = OffsetDateTime.now();
        var record = new DriverMedicalRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2027, 1, 15),
                DriverMedicalStatus.FIT,
                VisionTestStatus.PASSED,
                null,
                "Dr. John Doe",
                "MED-2026-001",
                "Fit for commercial driving",
                true,
                now,
                now,
                "admin",
                "admin"
        );

        assertTrue(record.isFit());
        assertFalse(record.isUnfit());
        assertTrue(record.isValidForPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2)));
    }

    @Test
    @DisplayName("Should reject record when validUntil precedes validFrom")
    void shouldRejectInvalidDateRange() {
        var now = OffsetDateTime.now();
        assertThrows(IllegalArgumentException.class, () -> new DriverMedicalRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2025, 1, 15),
                DriverMedicalStatus.FIT,
                VisionTestStatus.PASSED,
                null,
                "Dr. John Doe",
                "MED-2026-001",
                null,
                true,
                now,
                now,
                "admin",
                "admin"
        ));
    }

    @Test
    @DisplayName("Should evaluate FIT_WITH_RESTRICTIONS as fit but preserving restrictions")
    void shouldEvaluateFitWithRestrictions() {
        var now = OffsetDateTime.now();
        var record = new DriverMedicalRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2027, 1, 15),
                DriverMedicalStatus.FIT_WITH_RESTRICTIONS,
                VisionTestStatus.PASSED_WITH_CORRECTIVE_LENSES,
                "Must wear corrective lenses while driving",
                "Dr. Jane Smith",
                "MED-2026-002",
                null,
                true,
                now,
                now,
                "admin",
                "admin"
        );

        assertTrue(record.isFit());
        assertFalse(record.isUnfit());
        assertEquals("Must wear corrective lenses while driving", record.restrictions());
        assertEquals(VisionTestStatus.PASSED_WITH_CORRECTIVE_LENSES, record.visionTestStatus());
    }

    @Test
    @DisplayName("Should evaluate UNFIT and TEMPORARILY_UNFIT as unfit")
    void shouldEvaluateUnfitStatuses() {
        var now = OffsetDateTime.now();
        var unfit = new DriverMedicalRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2027, 1, 15),
                DriverMedicalStatus.UNFIT,
                VisionTestStatus.FAILED,
                "Severe cardiac condition",
                "Dr. Heart",
                "MED-2026-003",
                null,
                true,
                now,
                now,
                "admin",
                "admin"
        );

        assertFalse(unfit.isFit());
        assertTrue(unfit.isUnfit());
        assertFalse(unfit.isValidForPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2)));

        var tempUnfit = new DriverMedicalRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 3, 15),
                DriverMedicalStatus.TEMPORARILY_UNFIT,
                VisionTestStatus.PASSED,
                "Recovering from minor surgery",
                "Dr. Surgeon",
                "MED-2026-004",
                null,
                true,
                now,
                now,
                "admin",
                "admin"
        );

        assertFalse(tempUnfit.isFit());
        assertTrue(tempUnfit.isUnfit());
    }
}
