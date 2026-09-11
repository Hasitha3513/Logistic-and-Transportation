package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelExceptionUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionCorrectionExecutor;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionHandoff;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionSourceValidator;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionStore;
import com.transportlogistics.app.fuel.application.ports.out.FuelTransaction;
import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelExceptionServiceTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000038");
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000039");
    private static final UUID CASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-06T12:00:00Z");

    @Mock FuelExceptionStore store;
    @Mock FuelExceptionHandoff handoff;
    @Mock FuelExceptionCorrectionExecutor corrections;
    @Mock FuelExceptionSourceValidator sources;
    @Mock FuelTransaction transactions;
    private FuelExceptionService service;
    private FuelExceptionUseCase.Context context;

    @BeforeEach void setUp() {
        lenient().doAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get())
                .when(transactions).execute(any());
        service = new FuelExceptionService(store, handoff, corrections, sources, transactions,
                Clock.fixed(Instant.parse("2026-09-06T12:00:00Z"), ZoneOffset.UTC));
        context = new FuelExceptionUseCase.Context(TENANT, ACTOR, "reviewer", "correlation");
    }

    @Test void notesAcceptTwoThousandAndRejectTwoThousandOneCharacters() {
        when(store.find(TENANT, CASE_ID)).thenReturn(Optional.of(aCase(FuelExceptionCase.Impact.MEDIUM,
                FuelExceptionCase.HandoffStatus.NOT_REQUIRED, FuelExceptionCase.Lifecycle.UNDER_REVIEW, 0)));
        when(store.note(eq(TENANT), eq(CASE_ID), any(), eq(ACTOR), any())).thenAnswer(invocation ->
                new FuelExceptionUseCase.Note(UUID.randomUUID(), invocation.getArgument(2), ACTOR, NOW));

        service.addNote(context, CASE_ID, new FuelExceptionUseCase.Text("x".repeat(1999)));
        service.addNote(context, CASE_ID, new FuelExceptionUseCase.Text("x".repeat(2000)));
        assertThatThrownBy(() -> service.addNote(context, CASE_ID,
                new FuelExceptionUseCase.Text("x".repeat(2001)))).isInstanceOf(BusinessRuleException.class);
    }

    @Test void emergencyRefuelRequiresResolvedSameTenantReferences() {
        UUID source = UUID.randomUUID(), vehicle = UUID.randomUUID(), trip = UUID.randomUUID();
        when(sources.exists(TENANT, "FUEL_ISSUE", source)).thenReturn(true);
        when(sources.emergencyReferencesExist(TENANT, vehicle, trip, null)).thenReturn(false);
        var command = new FuelExceptionUseCase.Create(FuelExceptionCase.Category.EMERGENCY_REFUEL,
                "FUEL_ISSUE", source, FuelExceptionCase.Impact.HIGH, NOW, "Emergency review", Map.of(),
                vehicle, null, trip, null, null);

        assertThatThrownBy(() -> service.create(context, command)).isInstanceOf(NotFoundException.class);
        verify(store, never()).insert(any());
    }

    @Test void criticalResolutionRejectsMissingAndFailedHandoff() {
        when(store.find(TENANT, CASE_ID)).thenReturn(Optional.of(aCase(FuelExceptionCase.Impact.CRITICAL,
                        FuelExceptionCase.HandoffStatus.NOT_REQUIRED, FuelExceptionCase.Lifecycle.UNDER_REVIEW, 1)),
                Optional.of(aCase(FuelExceptionCase.Impact.CRITICAL, FuelExceptionCase.HandoffStatus.FAILED,
                        FuelExceptionCase.Lifecycle.UNDER_REVIEW, 1)));
        var resolve = new FuelExceptionUseCase.Resolve(1, FuelExceptionCase.Outcome.REFERRED_TO_OPERATIONS,
                "Reviewed outcome");

        assertThatThrownBy(() -> service.resolve(context, CASE_ID, resolve)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.resolve(context, CASE_ID, resolve)).isInstanceOf(BusinessRuleException.class);
    }

    @Test void criticalResolutionAllowsPublishedHandoff() {
        var published = aCase(FuelExceptionCase.Impact.CRITICAL, FuelExceptionCase.HandoffStatus.PUBLISHED,
                FuelExceptionCase.Lifecycle.UNDER_REVIEW, 1);
        var resolved = aCase(FuelExceptionCase.Impact.CRITICAL, FuelExceptionCase.HandoffStatus.PUBLISHED,
                FuelExceptionCase.Lifecycle.RESOLVED, 2);
        when(store.find(TENANT, CASE_ID)).thenReturn(Optional.of(published));
        when(store.update(eq(TENANT), eq(CASE_ID), eq(1L), eq(FuelExceptionCase.Lifecycle.RESOLVED),
                eq(false), eq(FuelExceptionCase.HandoffStatus.PUBLISHED),
                eq(FuelExceptionCase.Outcome.REFERRED_TO_OPERATIONS), eq("Reviewed outcome"), eq(ACTOR), any()))
                .thenReturn(resolved);

        assertThat(service.resolve(context, CASE_ID, new FuelExceptionUseCase.Resolve(1,
                FuelExceptionCase.Outcome.REFERRED_TO_OPERATIONS, "Reviewed outcome"))).isEqualTo(resolved);
        verify(store).history(eq(TENANT), eq(CASE_ID), eq("CASE_RESOLVED"), eq("UNDER_REVIEW"),
                eq("RESOLVED"), eq("Reviewed outcome"), eq(ACTOR), any());
    }

    @Test void retryAfterSuccessfulOwnerCommandIsNoop() {
        UUID correctionId = UUID.randomUUID();
        var correction = correction(correctionId, "APPLIED", 3);
        when(store.correction(TENANT, CASE_ID, correctionId)).thenReturn(Optional.of(correction));
        when(store.successfulCorrectionResult(TENANT, correctionId)).thenReturn(Optional.of("owner-result"));

        service.approve(context, CASE_ID, correctionId, new FuelExceptionUseCase.VersionedReason(3, "retry"));

        verify(corrections, never()).execute(any(), any(), any(), any(), any());
        verify(store).correctionAttempt(eq(TENANT), eq(correctionId), eq(correctionId), eq(ACTOR),
                eq("NOOP_REPLAY"), eq("owner-result"), eq(null), any());
        verify(store).history(eq(TENANT), eq(CASE_ID), eq("OWNER_COMMAND_NOOP_REPLAY"),
                eq(null), eq(null), eq("owner-result"), eq(ACTOR), any());
    }

    @Test void failedHandoffRetryReusesEventIdentityAndPublishes() {
        UUID eventId = UUID.randomUUID();
        var initial = aCase(FuelExceptionCase.Impact.CRITICAL, FuelExceptionCase.HandoffStatus.NOT_REQUIRED,
                FuelExceptionCase.Lifecycle.UNDER_REVIEW, 4);
        var failed = aCase(FuelExceptionCase.Impact.CRITICAL, FuelExceptionCase.HandoffStatus.FAILED,
                FuelExceptionCase.Lifecycle.UNDER_REVIEW, 5);
        var published = aCase(FuelExceptionCase.Impact.CRITICAL, FuelExceptionCase.HandoffStatus.PUBLISHED,
                FuelExceptionCase.Lifecycle.UNDER_REVIEW, 6);
        when(store.find(TENANT, CASE_ID)).thenReturn(Optional.of(initial), Optional.of(failed));
        when(store.handoff(TENANT, CASE_ID, "escalate", NOW)).thenReturn(eventId);
        when(store.handoffEvent(TENANT, CASE_ID)).thenReturn(Optional.of(eventId));
        when(store.update(eq(TENANT), eq(CASE_ID), eq(4L), any(), any(Boolean.class),
                eq(FuelExceptionCase.HandoffStatus.FAILED), eq(null), eq(null), eq(null), eq(NOW))).thenReturn(failed);
        when(store.update(eq(TENANT), eq(CASE_ID), eq(5L), any(), any(Boolean.class),
                eq(FuelExceptionCase.HandoffStatus.PUBLISHED), eq(null), eq(null), eq(null), eq(NOW))).thenReturn(published);
        doThrow(new IllegalStateException("controlled failure")).doNothing()
                .when(handoff).publish(any(), eq(eventId), any(), eq("correlation"));

        service.escalate(context, CASE_ID, new FuelExceptionUseCase.VersionedReason(4, "escalate"));
        service.escalate(context, CASE_ID, new FuelExceptionUseCase.VersionedReason(5, "retry"));

        verify(handoff, org.mockito.Mockito.times(2)).publish(any(), eq(eventId), any(), eq("correlation"));
        verify(store).handoffResult(TENANT, CASE_ID, "FAILED", "IllegalStateException", NOW);
        verify(store).handoffResult(TENANT, CASE_ID, "PUBLISHED", null, NOW);
    }

    private FuelExceptionCase aCase(FuelExceptionCase.Impact impact, FuelExceptionCase.HandoffStatus handoffStatus,
                                    FuelExceptionCase.Lifecycle lifecycle, long version) {
        return new FuelExceptionCase(CASE_ID, TENANT, FuelExceptionCase.Category.SUSPECTED_FUEL_LOSS, lifecycle,
                impact, "FUEL_ISSUE", UUID.randomUUID(), null, "Review case", Map.of(), null, null, null,
                null, null, NOW, true, handoffStatus, null, null, ACTOR, null, version, NOW, NOW);
    }

    private FuelExceptionUseCase.Correction correction(UUID id, String status, long version) {
        return new FuelExceptionUseCase.Correction(id, "BUNKER_STOCK_ADJUSTMENT",
                Map.of("tankId", UUID.randomUUID().toString(), "quantityDeltaLiters", "1", "reason", "review"),
                true, status, UUID.randomUUID(), ACTOR, "approved", "owner-result", null, version, NOW, NOW);
    }
}
