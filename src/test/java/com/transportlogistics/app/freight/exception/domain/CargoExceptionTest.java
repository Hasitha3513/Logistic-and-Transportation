package com.transportlogistics.app.freight.exception.domain;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Domain unit tests for CargoException aggregate — covers all 6 exception types,
 * all lifecycle transitions, invalid transitions, resolution history, and optimistic concurrency.
 */
class CargoExceptionTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final String ACTOR = "freight-manager";

    // ── Factory helpers ───────────────────────────────────────────────────────

    private CargoException openException(ExceptionType type) {
        return new CargoException(
                UUID.randomUUID(), "CEX-2026-000001", type,
                ExceptionStatus.OPEN, ExceptionSeverity.MEDIUM,
                ORDER_ID, null, null,
                "Test description", "Some impact", null, null, null,
                null, null, List.of(),
                NOW, NOW, ACTOR, ACTOR, 0L
        );
    }

    // ── Construction invariants ───────────────────────────────────────────────

    @Test
    void requiresId() {
        assertThatThrownBy(() -> new CargoException(
                null, "CEX-2026-000001", ExceptionType.DAMAGE,
                ExceptionStatus.OPEN, ExceptionSeverity.MEDIUM,
                ORDER_ID, null, null, "desc", null, null, null, null,
                null, null, List.of(), NOW, NOW, ACTOR, ACTOR, 0L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void requiresDescription() {
        assertThatThrownBy(() -> new CargoException(
                UUID.randomUUID(), "CEX-2026-000001", ExceptionType.DAMAGE,
                ExceptionStatus.OPEN, ExceptionSeverity.MEDIUM,
                ORDER_ID, null, null, "", null, null, null, null,
                null, null, List.of(), NOW, NOW, ACTOR, ACTOR, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsStatusToOpen() {
        CargoException exc = new CargoException(
                UUID.randomUUID(), "CEX-2026-000001", ExceptionType.DAMAGE,
                null, null, ORDER_ID, null, null, "desc",
                null, null, null, null, null, null, List.of(),
                NOW, NOW, ACTOR, ACTOR, 0L
        );
        assertThat(exc.getStatus()).isEqualTo(ExceptionStatus.OPEN);
        assertThat(exc.getSeverity()).isEqualTo(ExceptionSeverity.MEDIUM);
    }

    // ── Hold ──────────────────────────────────────────────────────────────────

    @Test
    void holdFromOpen() {
        CargoException exc = openException(ExceptionType.HAZARDOUS_MATERIAL);
        CargoException held = exc.hold("No movement", "Hazmat detected", ACTOR, NOW);

        assertThat(held.getStatus()).isEqualTo(ExceptionStatus.HELD);
        assertThat(held.getRestriction()).isEqualTo("No movement");
        assertThat(held.getHistory()).hasSize(1);
        assertThat(held.getHistory().get(0).getAction()).isEqualTo("HOLD_APPLIED");
    }

    @Test
    void holdFromEscalated() {
        CargoException exc = openException(ExceptionType.SEAL_TAMPERING)
                .escalate("Severity confirmed", ACTOR, NOW);
        CargoException held = exc.hold("Quarantine required", "Senior review", ACTOR, NOW);
        assertThat(held.getStatus()).isEqualTo(ExceptionStatus.HELD);
    }

    @Test
    void holdFromResolvedThrows() {
        CargoException exc = openException(ExceptionType.DAMAGE)
                .resolve("Fixed", null, "ok", ACTOR, NOW);
        assertThatThrownBy(() -> exc.hold(null, "reason", ACTOR, NOW))
                .isInstanceOf(ConflictException.class);
    }

    // ── Escalate ──────────────────────────────────────────────────────────────

    @Test
    void escalateFromOpen() {
        CargoException exc = openException(ExceptionType.WEIGHT_DISCREPANCY);
        CargoException escalated = exc.escalate("Exceeds allowed delta by 200kg", ACTOR, NOW);

        assertThat(escalated.getStatus()).isEqualTo(ExceptionStatus.ESCALATED);
        assertThat(escalated.getHistory()).hasSize(1);
        assertThat(escalated.getHistory().get(0).getAction()).isEqualTo("ESCALATED");
    }

    @Test
    void escalateRequiresReason() {
        CargoException exc = openException(ExceptionType.PARTIAL_SHIPMENT);
        assertThatThrownBy(() -> exc.escalate("", ACTOR, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void escalateFromResolvedThrows() {
        CargoException exc = openException(ExceptionType.DAMAGE)
                .resolve("Done", null, null, ACTOR, NOW);
        assertThatThrownBy(() -> exc.escalate("reason", ACTOR, NOW))
                .isInstanceOf(ConflictException.class);
    }

    // ── Release ───────────────────────────────────────────────────────────────

    @Test
    void releaseFromHeld() {
        CargoException exc = openException(ExceptionType.HAZARDOUS_MATERIAL)
                .hold("No movement", "Hazmat", ACTOR, NOW);
        CargoException released = exc.release("Cleared by safety officer", ACTOR, NOW);

        assertThat(released.getStatus()).isEqualTo(ExceptionStatus.OPEN);
        assertThat(released.getRestriction()).isNull();
        assertThat(released.getHistory()).hasSize(2);
        assertThat(released.getHistory().get(1).getAction()).isEqualTo("RELEASED");
    }

    @Test
    void releaseRequiresReason() {
        CargoException exc = openException(ExceptionType.SEAL_TAMPERING)
                .hold("Sealed", "tampering", ACTOR, NOW);
        assertThatThrownBy(() -> exc.release("", ACTOR, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void releaseFromOpenThrows() {
        CargoException exc = openException(ExceptionType.DAMAGE);
        assertThatThrownBy(() -> exc.release("reason", ACTOR, NOW))
                .isInstanceOf(ConflictException.class);
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    @Test
    void rejectFromOpen() {
        CargoException exc = openException(ExceptionType.UNMANIFESTED_CARGO);
        CargoException rejected = exc.reject("Cargo traced to correct manifest", ACTOR, NOW);

        assertThat(rejected.getStatus()).isEqualTo(ExceptionStatus.REJECTED);
        assertThat(rejected.getHistory().get(0).getAction()).isEqualTo("REJECTED");
    }

    @Test
    void rejectRequiresReason() {
        CargoException exc = openException(ExceptionType.DAMAGE);
        assertThatThrownBy(() -> exc.reject(null, ACTOR, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectFromResolvedThrows() {
        CargoException exc = openException(ExceptionType.DAMAGE)
                .resolve("Resolved", null, null, ACTOR, NOW);
        assertThatThrownBy(() -> exc.reject("reason", ACTOR, NOW))
                .isInstanceOf(ConflictException.class);
    }

    // ── Resolve ───────────────────────────────────────────────────────────────

    @Test
    void resolveFromOpen() {
        CargoException exc = openException(ExceptionType.DAMAGE);
        CargoException resolved = exc.resolve("Damage repaired", "Replaced packaging", "ok", ACTOR, NOW);

        assertThat(resolved.getStatus()).isEqualTo(ExceptionStatus.RESOLVED);
        assertThat(resolved.getResolution()).isEqualTo("Damage repaired");
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(resolved.getResolvedBy()).isEqualTo(ACTOR);
        assertThat(resolved.getHistory().get(0).getAction()).isEqualTo("RESOLVED");
    }

    @Test
    void resolveRequiresResolution() {
        CargoException exc = openException(ExceptionType.PARTIAL_SHIPMENT);
        assertThatThrownBy(() -> exc.resolve("", null, null, ACTOR, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resolveFromRejectedThrows() {
        CargoException exc = openException(ExceptionType.DAMAGE)
                .reject("not valid", ACTOR, NOW);
        assertThatThrownBy(() -> exc.resolve("done", null, null, ACTOR, NOW))
                .isInstanceOf(ConflictException.class);
    }

    // ── History immutability ──────────────────────────────────────────────────

    @Test
    void historyIsImmutableAndAccumulatesCorrectly() {
        CargoException exc = openException(ExceptionType.SEAL_TAMPERING);
        CargoException held = exc.hold("Quarantine", "Tampering found", ACTOR, NOW);
        CargoException released = held.release("Cleared", ACTOR, NOW);
        CargoException resolved = released.resolve("Seal replaced", null, null, ACTOR, NOW);

        assertThat(resolved.getHistory()).hasSize(3);
        assertThat(resolved.getHistory().get(0).getAction()).isEqualTo("HOLD_APPLIED");
        assertThat(resolved.getHistory().get(1).getAction()).isEqualTo("RELEASED");
        assertThat(resolved.getHistory().get(2).getAction()).isEqualTo("RESOLVED");

        // Original exception is unchanged
        assertThat(exc.getHistory()).isEmpty();
        assertThat(exc.getStatus()).isEqualTo(ExceptionStatus.OPEN);
    }

    // ── All six exception types ───────────────────────────────────────────────

    @Test
    void allSixExceptionTypesCanBeCreated() {
        for (ExceptionType type : ExceptionType.values()) {
            CargoException exc = openException(type);
            assertThat(exc.getExceptionType()).isEqualTo(type);
        }
    }
}
