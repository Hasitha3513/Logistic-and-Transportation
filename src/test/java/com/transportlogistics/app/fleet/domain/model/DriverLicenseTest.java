package com.transportlogistics.app.fleet.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DriverLicenseTest {
    @Test
    void issueAndExpiryDatesAreRequired() {
        assertThrows(IllegalArgumentException.class, () -> license(null, LocalDate.of(2027, 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> license(LocalDate.of(2026, 1, 1), null));
    }

    @Test
    void expiryMustBeStrictlyLaterThanIssueDate() {
        var issue = LocalDate.of(2026, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> license(issue, issue));
        assertThrows(IllegalArgumentException.class, () -> license(issue, issue.minusDays(1)));
    }

    @Test
    void expiryAndClassDetermineValidity() {
        var license = license(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1));

        assertTrue(license.isValidFor("B", LocalDate.of(2026, 1, 1)));
        assertFalse(license.isValidFor("C", LocalDate.of(2026, 1, 1)));
        assertTrue(license.isExpiredOn(LocalDate.of(2026, 1, 2)));
    }

    private DriverLicense license(LocalDate issueDate, LocalDate expiryDate) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new DriverLicense(UUID.randomUUID(), UUID.randomUUID(), "dl-123", "b", issueDate, expiryDate,
                DriverLicenseStatus.ACTIVE, true, now, now, "tester", "tester");
    }
}
