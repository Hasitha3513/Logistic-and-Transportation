package com.transportlogistics.app.fuel.domain.service;

import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelLimitPolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FuelIssuePolicyTest {
    private final FuelIssuePolicy policy = new FuelIssuePolicy();

    @Test
    void acceptsPositiveQuantityAndConfiguredLimit() {
        var issue = issue(FuelIssueStatus.DRAFT, "50.000");
        assertDoesNotThrow(() -> policy.validateDraft(issue));
        assertDoesNotThrow(() -> policy.enforceLimits(issue,
                List.of(new FuelLimitPolicy(UUID.randomUUID(), null, new BigDecimal("75"), true))));
    }

    @Test
    void rejectsZeroAndNegativeQuantitiesWithStableCode() {
        for (var quantity : List.of("0", "-1")) {
            var error = assertThrows(BusinessRuleException.class,
                    () -> policy.validateDraft(issue(FuelIssueStatus.DRAFT, quantity)));
            assertEquals("INVALID_FUEL_QUANTITY", error.code());
        }
    }

    @Test
    void rejectsLimitExceeded() {
        var error = assertThrows(BusinessRuleException.class, () -> policy.enforceLimits(
                issue(FuelIssueStatus.DRAFT, "51"),
                List.of(new FuelLimitPolicy(UUID.randomUUID(), null, new BigDecimal("50"), true))));
        assertEquals("FUEL_LIMIT_EXCEEDED", error.code());
    }

    @Test
    void lifecycleDoesNotPermitArbitraryTransitions() {
        assertThrows(ConflictException.class, () -> policy.requireAuthorizable(issue(FuelIssueStatus.DRAFT, "10")));
        assertThrows(ConflictException.class, () -> policy.requireIssuable(issue(FuelIssueStatus.PENDING_AUTHORIZATION, "10")));
        assertThrows(ConflictException.class, () -> policy.requireEditable(issue(FuelIssueStatus.ISSUED, "10")));
    }

    @Test
    void cancellationAfterSubmissionRequiresReason() {
        var error = assertThrows(BusinessRuleException.class,
                () -> policy.requireCancellable(issue(FuelIssueStatus.AUTHORIZED, "10"), " "));
        assertEquals("FUEL_CANCELLATION_REASON_REQUIRED", error.code());
    }

    private FuelIssue issue(FuelIssueStatus status, String quantity) {
        var now = OffsetDateTime.parse("2026-08-15T00:00:00Z");
        return new FuelIssue(UUID.randomUUID(), "FUEL-2026-000001", UUID.randomUUID(), null, null, "DIESEL",
                new BigDecimal(quantity), null, null, UUID.randomUUID(), new BigDecimal("1000"), null, now,
                status, UUID.randomUUID(), null, null, null, now, now);
    }
}
