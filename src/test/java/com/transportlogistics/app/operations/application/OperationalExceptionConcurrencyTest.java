package com.transportlogistics.app.operations.application;

import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.operations.domain.model.CorrectiveAction;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
import com.transportlogistics.app.operations.domain.model.RootCauseAnalysis;
import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;
import com.transportlogistics.app.operations.ports.outbound.CorrectiveActionRepository;
import com.transportlogistics.app.operations.ports.outbound.OperationalAssigneeDirectory;
import com.transportlogistics.app.operations.ports.outbound.OperationalExceptionCaseRepository;
import com.transportlogistics.app.operations.ports.outbound.OperationalExceptionHistoryRepository;
import com.transportlogistics.app.operations.ports.outbound.OperationsEventPublisher;
import com.transportlogistics.app.operations.ports.outbound.OperationsTransaction;
import com.transportlogistics.app.operations.ports.outbound.RootCauseAnalysisRepository;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationalExceptionConcurrencyTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-04T02:00:00Z");
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private OperationalExceptionCaseRepository cases;
    private CorrectiveActionRepository actions;
    private RootCauseAnalysisRepository rcas;
    private OperationalExceptionHistoryRepository history;
    private OperationsEventPublisher events;
    private OperationalExceptionService service;
    private OperationalExceptionUseCase.Context context;

    @BeforeEach
    void setUp() {
        cases = mock(OperationalExceptionCaseRepository.class);
        actions = mock(CorrectiveActionRepository.class);
        rcas = mock(RootCauseAnalysisRepository.class);
        history = mock(OperationalExceptionHistoryRepository.class);
        OperationalAssigneeDirectory assignees = mock(OperationalAssigneeDirectory.class);
        events = mock(OperationsEventPublisher.class);
        OperationsTransaction transactions = mock(OperationsTransaction.class);
        when(transactions.execute(any())).thenAnswer(invocation ->
            ((Supplier<?>) invocation.getArgument(0)).get());
        when(cases.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(actions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(rcas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(actions.findActionsByCase(any(), any())).thenReturn(List.of());
        when(rcas.findRcaByCase(any(), any())).thenReturn(Optional.empty());
        when(assignees.activeRole(anyString())).thenReturn(true);
        service = new OperationalExceptionService(cases, actions, rcas, history, assignees, events,
            transactions, Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
        context = new OperationalExceptionUseCase.Context(TENANT, ACTOR, "operator", "concurrency-test");
    }

    @Test
    void duplicateIntakeReusesTheFirstCase() {
        var stored = new AtomicReference<OperationalExceptionCase>();
        var fact = fact(UUID.randomUUID(), NOW.minusHours(1), OperationalExceptionFactV1.Severity.MEDIUM);
        when(cases.findBySourceEvent(TENANT, fact.eventId())).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(cases.save(any())).thenAnswer(invocation -> {
            OperationalExceptionCase value = invocation.getArgument(0);
            stored.set(value);
            return value;
        });

        OperationalExceptionCase first = service.intake(fact);
        OperationalExceptionCase replay = service.intake(fact);

        assertThat(replay.id()).isEqualTo(first.id());
        verify(cases).save(first);
    }

    @Test
    void competingAssignmentsAndRepeatedEscalationRejectStaleVersions() {
        OperationalExceptionCase value = exceptionCase(OperationalExceptionCase.Severity.MEDIUM, NOW.minusHours(1));
        when(cases.find(TENANT, value.id())).thenReturn(Optional.of(value));

        service.assign(context, value.id(), new OperationalExceptionUseCase.AssignCommand(0,
            OperationalExceptionCase.AssignmentType.ROLE_QUEUE, null, "OPERATIONS_SAFETY_QUEUE", "take ownership"));
        assertThatThrownBy(() -> service.assign(context, value.id(), new OperationalExceptionUseCase.AssignCommand(0,
            OperationalExceptionCase.AssignmentType.ROLE_QUEUE, null, "OPERATIONS_QUEUE", "competing owner")))
            .isInstanceOf(ConflictException.class);

        service.escalate(context, value.id(), new OperationalExceptionUseCase.ReasonCommand(1, "manual review"));
        assertThatThrownBy(() -> service.escalate(context, value.id(),
            new OperationalExceptionUseCase.ReasonCommand(1, "same level retry")))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void closeWinsOverAStaleEscalationAttempt() {
        OperationalExceptionCase value = exceptionCase(OperationalExceptionCase.Severity.LOW, NOW.minusHours(1));
        when(cases.find(TENANT, value.id())).thenReturn(Optional.of(value));
        service.start(context, value.id(), new OperationalExceptionUseCase.VersionCommand(0));
        service.resolve(context, value.id(), new OperationalExceptionUseCase.ResolveCommand(1,
            "Source result verified", "ROUTING:RESOLVED"));
        service.close(context, value.id(), new OperationalExceptionUseCase.VersionCommand(2));

        assertThatThrownBy(() -> service.escalate(context, value.id(),
            new OperationalExceptionUseCase.ReasonCommand(2, "late worker")))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void resolutionWaitsForCompetingCorrectiveActionCompletion() {
        OperationalExceptionCase value = exceptionCase(OperationalExceptionCase.Severity.LOW, NOW.minusHours(1));
        CorrectiveAction action = CorrectiveAction.open(UUID.randomUUID(), TENANT, value.id(),
            CorrectiveAction.Type.CORRECTIVE, "Verify source correction",
            OperationalExceptionCase.AssignmentType.ROLE_QUEUE, null, "OPERATIONS_QUEUE", null, null, NOW);
        when(cases.find(TENANT, value.id())).thenReturn(Optional.of(value));
        when(actions.find(TENANT, value.id(), action.id())).thenReturn(Optional.of(action));
        when(actions.findActionsByCase(TENANT, value.id())).thenReturn(List.of(action));
        service.start(context, value.id(), new OperationalExceptionUseCase.VersionCommand(0));

        assertThatThrownBy(() -> service.resolve(context, value.id(), new OperationalExceptionUseCase.ResolveCommand(1,
            "Premature resolution", null))).isInstanceOf(BusinessRuleException.class);
        service.completeCorrectiveAction(context, value.id(), action.id(),
            new OperationalExceptionUseCase.VersionCommand(0));
        service.resolve(context, value.id(), new OperationalExceptionUseCase.ResolveCommand(1,
            "Correction completed", null));

        assertThat(value.status()).isEqualTo(OperationalExceptionCase.Status.RESOLVED);
    }

    @Test
    void rcaApprovalWinsBeforeCloseAndRejectsAStaleApprovalReplay() {
        OperationalExceptionCase value = exceptionCase(OperationalExceptionCase.Severity.HIGH, NOW.minusHours(1));
        UUID approver = UUID.randomUUID();
        RootCauseAnalysis rca = RootCauseAnalysis.create(UUID.randomUUID(), TENANT, value.id(),
            RootCauseAnalysis.CauseCategory.PROCESS, "CONTROL_GAP", "Control gap", null, ACTOR, NOW);
        when(cases.find(TENANT, value.id())).thenReturn(Optional.of(value));
        when(rcas.findRcaByCase(TENANT, value.id())).thenReturn(Optional.of(rca));
        service.start(context, value.id(), new OperationalExceptionUseCase.VersionCommand(0));
        service.resolve(context, value.id(), new OperationalExceptionUseCase.ResolveCommand(1,
            "Corrected and verified", null));
        var approverContext = new OperationalExceptionUseCase.Context(TENANT, approver, "approver", "concurrency-test");
        service.approveRca(approverContext, value.id(), new OperationalExceptionUseCase.RcaApprovalCommand(2, 0));
        service.close(approverContext, value.id(), new OperationalExceptionUseCase.VersionCommand(2));

        assertThatThrownBy(() -> service.approveRca(approverContext, value.id(),
            new OperationalExceptionUseCase.RcaApprovalCommand(3, 0))).isInstanceOf(ConflictException.class);
        assertThat(value.status()).isEqualTo(OperationalExceptionCase.Status.CLOSED);
    }

    @Test
    void slaWorkerSkipsAClosedCandidateAndReopenSchedulesFutureWork() {
        OperationalExceptionCase value = exceptionCase(OperationalExceptionCase.Severity.LOW, NOW.minusDays(4));
        when(cases.find(TENANT, value.id())).thenReturn(Optional.of(value));
        service.start(context, value.id(), new OperationalExceptionUseCase.VersionCommand(0));
        service.resolve(context, value.id(), new OperationalExceptionUseCase.ResolveCommand(1, "Resolved", null));
        service.close(context, value.id(), new OperationalExceptionUseCase.VersionCommand(2));
        when(cases.findDue(TENANT, NOW, 50)).thenReturn(List.of(value));

        assertThat(service.scanDue(TENANT)).isZero();
        service.reopen(context, value.id(), new OperationalExceptionUseCase.ReasonCommand(3, "Issue recurred"));
        assertThat(value.nextEscalationAt()).isAfter(NOW);
    }

    private static OperationalExceptionCase exceptionCase(OperationalExceptionCase.Severity severity,
                                                            OffsetDateTime occurredAt) {
        return OperationalExceptionCase.open(UUID.randomUUID(), TENANT, "OEX-0123456789AB", UUID.randomUUID(),
            OperationalExceptionCase.SourceModule.ROUTING, "ACCIDENT", UUID.randomUUID(), occurredAt,
            "ROUTE_DISRUPTION_CREATED", "concurrency-test", OperationalExceptionCase.Category.OPERATIONAL,
            severity, "OPERATIONS_QUEUE", NOW);
    }

    private static OperationalExceptionFactV1 fact(UUID eventId, OffsetDateTime occurredAt,
                                                    OperationalExceptionFactV1.Severity severity) {
        return new OperationalExceptionFactV1(eventId, TENANT, OperationalExceptionFactV1.SourceModule.ROUTING,
            "ACCIDENT", UUID.randomUUID(), occurredAt, severity, OperationalExceptionFactV1.Category.OPERATIONAL,
            "ROUTE_DISRUPTION_CREATED", Map.of(), "concurrency-test");
    }
}
