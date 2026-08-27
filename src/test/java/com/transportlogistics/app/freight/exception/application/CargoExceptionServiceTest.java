package com.transportlogistics.app.freight.exception.application;

import com.transportlogistics.app.freight.exception.domain.CargoException;
import com.transportlogistics.app.freight.exception.domain.ExceptionSeverity;
import com.transportlogistics.app.freight.exception.domain.ExceptionStatus;
import com.transportlogistics.app.freight.exception.domain.ExceptionType;
import com.transportlogistics.app.freight.exception.ports.inbound.CargoExceptionUseCase;
import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionNumberGenerator;
import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionRepository;
import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionTransaction;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CargoExceptionServiceTest {

    @Mock CargoExceptionRepository repository;
    @Mock CargoExceptionNumberGenerator numberGenerator;
    @Mock CargoExceptionTransaction transactions;
    @Mock FreightOrderLookup freightOrderLookup;

    private CargoExceptionService service;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID EXCEPTION_ID = UUID.randomUUID();
    private static final String ACTOR = "test-actor";
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        service = new CargoExceptionService(
                repository, numberGenerator, transactions, freightOrderLookup, Clock.systemUTC());
        // Make transaction port execute directly
        when(transactions.execute(any())).thenAnswer(inv -> inv.getArgument(0, java.util.function.Supplier.class).get());
    }

    private CargoException makeOpenException() {
        return new CargoException(
                EXCEPTION_ID, "CEX-2026-000001", ExceptionType.DAMAGE,
                ExceptionStatus.OPEN, ExceptionSeverity.MEDIUM,
                ORDER_ID, null, null, "Test damage", null, null, null, null,
                null, null, List.of(), NOW, NOW, ACTOR, ACTOR, 0L
        );
    }

    // ── record ────────────────────────────────────────────────────────────────

    @Test
    void record_savesNewException() {
        when(numberGenerator.nextExceptionNumber()).thenReturn("CEX-2026-000001");
        when(freightOrderLookup.find(ORDER_ID)).thenReturn(Optional.of(stubOrder()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CargoExceptionUseCase.RecordExceptionCommand(
                ExceptionType.DAMAGE, ExceptionSeverity.HIGH,
                ORDER_ID, null, null,
                "Cargo damaged during loading", null, null, null);

        CargoException result = service.record(cmd, ACTOR);

        assertThat(result.getExceptionType()).isEqualTo(ExceptionType.DAMAGE);
        assertThat(result.getStatus()).isEqualTo(ExceptionStatus.OPEN);
        assertThat(result.getSeverity()).isEqualTo(ExceptionSeverity.HIGH);
        verify(repository).save(any());
    }

    @Test
    void record_throwsWhenFreightOrderNotFound() {
        when(freightOrderLookup.find(ORDER_ID)).thenReturn(Optional.empty());

        var cmd = new CargoExceptionUseCase.RecordExceptionCommand(
                ExceptionType.DAMAGE, null, ORDER_ID, null, null, "desc", null, null, null);

        assertThatThrownBy(() -> service.record(cmd, ACTOR))
                .isInstanceOf(NotFoundException.class);
    }

    // ── get ───────────────────────────────────────────────────────────────────

    @Test
    void get_returnsExistingException() {
        when(repository.findById(EXCEPTION_ID)).thenReturn(Optional.of(makeOpenException()));
        CargoException result = service.get(EXCEPTION_ID);
        assertThat(result.getId()).isEqualTo(EXCEPTION_ID);
    }

    @Test
    void get_throwsWhenNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    // ── hold ──────────────────────────────────────────────────────────────────

    @Test
    void hold_appliesHoldAndSaves() {
        when(repository.findById(EXCEPTION_ID)).thenReturn(Optional.of(makeOpenException()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CargoExceptionUseCase.HoldExceptionCommand("No movement", "Hazmat detected", 0L);
        CargoException result = service.hold(EXCEPTION_ID, cmd, ACTOR);

        assertThat(result.getStatus()).isEqualTo(ExceptionStatus.HELD);
        verify(repository).save(any());
    }

    @Test
    void hold_throwsOnStaleVersion() {
        when(repository.findById(EXCEPTION_ID)).thenReturn(Optional.of(makeOpenException()));

        var cmd = new CargoExceptionUseCase.HoldExceptionCommand(null, "reason", 99L);
        assertThatThrownBy(() -> service.hold(EXCEPTION_ID, cmd, ACTOR))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stale version");
    }

    // ── escalate ──────────────────────────────────────────────────────────────

    @Test
    void escalate_appliesEscalationAndSaves() {
        when(repository.findById(EXCEPTION_ID)).thenReturn(Optional.of(makeOpenException()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CargoExceptionUseCase.EscalateExceptionCommand("Exceeds threshold", 0L);
        CargoException result = service.escalate(EXCEPTION_ID, cmd, ACTOR);

        assertThat(result.getStatus()).isEqualTo(ExceptionStatus.ESCALATED);
    }

    // ── resolve ───────────────────────────────────────────────────────────────

    @Test
    void resolve_appliesResolutionWithHistory() {
        when(repository.findById(EXCEPTION_ID)).thenReturn(Optional.of(makeOpenException()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CargoExceptionUseCase.ResolveExceptionCommand(
                "Cargo repackaged and cleared", null, "Inspection passed", 0L);
        CargoException result = service.resolve(EXCEPTION_ID, cmd, ACTOR);

        assertThat(result.getStatus()).isEqualTo(ExceptionStatus.RESOLVED);
        assertThat(result.getResolution()).isEqualTo("Cargo repackaged and cleared");
        assertThat(result.getHistory()).hasSize(1);
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void list_delegatesWithSafeDefaults() {
        when(repository.findAll(null, null, null, null, 0, 20)).thenReturn(List.of());
        List<CargoException> result = service.list(null, null, null, null, 0, 0);
        assertThat(result).isEmpty();
        verify(repository).findAll(null, null, null, null, 0, 20);
    }

    // ── private helper ────────────────────────────────────────────────────────

    private FreightOrderLookup.OrderReference stubOrder() {
        return new FreightOrderLookup.OrderReference(ORDER_ID, "FO-2026-000001", List.of());
    }
}
