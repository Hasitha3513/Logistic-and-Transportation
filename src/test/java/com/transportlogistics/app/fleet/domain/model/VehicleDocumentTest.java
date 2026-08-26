package com.transportlogistics.app.fleet.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VehicleDocumentTest {
    @Test
    void rejectsExpiryBeforeIssueDate() {
        assertThrows(IllegalArgumentException.class, () -> document(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31), true));
    }

    @Test
    void expiredMandatoryActiveDocumentBlocksDispatch() {
        var document = document(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), true);

        assertTrue(document.blocksDispatchOn(LocalDate.of(2026, 1, 1)));
        assertFalse(document.blocksDispatchOn(LocalDate.of(2025, 12, 31)));
    }

    @Test
    void nonMandatoryDocumentDoesNotBlockDispatchWhenExpired() {
        assertFalse(document(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), false)
                .blocksDispatchOn(LocalDate.of(2026, 1, 1)));
    }

    private VehicleDocument document(LocalDate issue, LocalDate expiry, boolean mandatory) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new VehicleDocument(UUID.randomUUID(), UUID.randomUUID(), "insurance", "POL-123", issue, expiry,
                null, mandatory, VehicleDocumentStatus.ACTIVE, true, now, now, "tester", "tester");
    }
}
