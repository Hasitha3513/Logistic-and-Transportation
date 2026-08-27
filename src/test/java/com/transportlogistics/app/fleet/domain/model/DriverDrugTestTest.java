package com.transportlogistics.app.fleet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DriverDrugTestTest {

    @Test
    @DisplayName("Should create scheduled drug test with PENDING result")
    void shouldCreateScheduledTest() {
        var now = OffsetDateTime.now();
        var test = new DriverDrugTest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DrugTestType.RANDOM,
                LocalDate.of(2026, 5, 20),
                null,
                null,
                DrugTestResult.PENDING,
                DrugTestStatus.SCHEDULED,
                "Quest Diagnostics",
                "REF-2026-001",
                "Routine random test",
                false,
                null,
                true,
                now,
                now,
                "admin",
                "admin"
        );

        assertEquals(DrugTestStatus.SCHEDULED, test.status());
        assertEquals(DrugTestResult.PENDING, test.result());
        assertFalse(test.isBlocking());
    }

    @Test
    @DisplayName("Should evaluate POSITIVE test as blocking until return-to-duty clearance")
    void shouldEvaluatePositiveTestAsBlocking() {
        var now = OffsetDateTime.now();
        var positiveTest = new DriverDrugTest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DrugTestType.POST_INCIDENT,
                LocalDate.of(2026, 5, 20),
                now.minusDays(1),
                LocalDate.of(2026, 5, 21),
                DrugTestResult.POSITIVE,
                DrugTestStatus.COMPLETED,
                "LabCorp",
                "REF-2026-002",
                "Positive for prohibited substances",
                true,
                null,
                true,
                now,
                now,
                "admin",
                "admin"
        );

        assertTrue(positiveTest.isBlocking());

        // Once cleared, should no longer block
        var clearedTest = new DriverDrugTest(
                positiveTest.id(),
                positiveTest.driverId(),
                positiveTest.testType(),
                positiveTest.scheduledDate(),
                positiveTest.sampleCollectedAt(),
                positiveTest.resultDate(),
                positiveTest.result(),
                positiveTest.status(),
                positiveTest.laboratoryOrProvider(),
                positiveTest.referenceNumber(),
                positiveTest.remarks(),
                true,
                OffsetDateTime.now(),
                true,
                positiveTest.createdAt(),
                OffsetDateTime.now(),
                "admin",
                "admin"
        );

        assertFalse(clearedTest.isBlocking());
    }

    @Test
    @DisplayName("Should evaluate NEGATIVE test as non-blocking")
    void shouldEvaluateNegativeTestAsNonBlocking() {
        var now = OffsetDateTime.now();
        var negativeTest = new DriverDrugTest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DrugTestType.PRE_EMPLOYMENT,
                LocalDate.of(2026, 5, 20),
                now.minusDays(2),
                LocalDate.of(2026, 5, 21),
                DrugTestResult.NEGATIVE,
                DrugTestStatus.COMPLETED,
                "LabCorp",
                "REF-2026-003",
                "All panels negative",
                false,
                null,
                true,
                now,
                now,
                "admin",
                "admin"
        );

        assertFalse(negativeTest.isBlocking());
    }
}
