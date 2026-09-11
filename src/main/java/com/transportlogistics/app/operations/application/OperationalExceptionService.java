package com.transportlogistics.app.operations.application;

import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.operations.domain.model.AssignmentHistory;
import com.transportlogistics.app.operations.domain.model.CorrectiveAction;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionHistory;
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
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class OperationalExceptionService implements OperationalExceptionUseCase {
    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private final OperationalExceptionCaseRepository cases;
    private final CorrectiveActionRepository actions;
    private final RootCauseAnalysisRepository rcas;
    private final OperationalExceptionHistoryRepository history;
    private final OperationalAssigneeDirectory assignees;
    private final OperationsEventPublisher events;
    private final OperationsTransaction transactions;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @SuppressWarnings("java:S107")
    public OperationalExceptionService(OperationalExceptionCaseRepository cases, CorrectiveActionRepository actions,
                                       RootCauseAnalysisRepository rcas,
                                       OperationalExceptionHistoryRepository history,
                                       OperationalAssigneeDirectory assignees, OperationsEventPublisher events,
                                       OperationsTransaction transactions, Clock clock) {
        this.cases = Objects.requireNonNull(cases);
        this.actions = Objects.requireNonNull(actions);
        this.rcas = Objects.requireNonNull(rcas);
        this.history = Objects.requireNonNull(history);
        this.assignees = Objects.requireNonNull(assignees);
        this.events = Objects.requireNonNull(events);
        this.transactions = Objects.requireNonNull(transactions);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public OperationalExceptionCase intake(OperationalExceptionFactV1 fact) {
        Objects.requireNonNull(fact);
        return transactions.execute(() -> cases.findBySourceEvent(fact.tenantId(), fact.eventId())
            .orElseGet(() -> createFromFact(fact)));
    }

    private OperationalExceptionCase createFromFact(OperationalExceptionFactV1 fact) {
        OffsetDateTime now = now();
        var severity = OperationalExceptionCase.Severity.valueOf(fact.severityCandidate().name());
        var category = OperationalExceptionCase.Category.valueOf(fact.categoryCandidate().name());
        String role = queue(category);
        if (!assignees.activeRole(role)) {
            throw new BusinessRuleException("OPERATIONAL_EXCEPTION_ASSIGNMENT_INVALID",
                "Configured operational role queue is not active");
        }
        var created = OperationalExceptionCase.open(UUID.randomUUID(), fact.tenantId(), nextReference(fact.tenantId()),
            fact.eventId(), OperationalExceptionCase.SourceModule.valueOf(fact.sourceModule().name()),
            fact.sourceType(), fact.sourceId(), fact.occurredAt(), fact.summaryCode(), fact.correlationId(),
            category, severity, role, now);
        OperationalExceptionCase saved = cases.save(created);
        append(saved, "OPENED", null, saved.status().name(), null, system(fact), now);
        append(saved, "AUTO_ASSIGNED", null, role, "CATEGORY_SEVERITY_POLICY", system(fact), now);
        if (severity == OperationalExceptionCase.Severity.CRITICAL) {
            append(saved, "ESCALATED", "L0", "L1", "CRITICAL_INTAKE", system(fact), now);
            events.publishEscalation(saved, now);
        }
        return saved;
    }

    @Override
    public PageResult<OperationalExceptionCase> list(Context context, Query query) {
        validateContext(context);
        int size = bounded(query.size(), 20, 100);
        int page = Math.max(0, query.page());
        String sort = allowedSort(query.sort());
        return cases.search(context.tenantId(), new Query(query.status(), query.severity(), query.category(),
            query.sourceModule(), query.assignedUserId(), clean(query.assignedRoleCode()), query.slaStatus(),
            query.openedFrom(), query.openedTo(), clean(query.search()), sort, query.descending(), page, size), now());
    }

    @Override public CaseDetail get(Context context, UUID id) { return detail(required(context, id)); }

    @Override
    public PageResult<OperationalExceptionHistory> history(Context context, UUID id, int page, int size) {
        required(context, id);
        return history.findByCase(context.tenantId(), id, Math.max(0, page), bounded(size, 50, 200));
    }

    @Override
    public CaseDetail classify(Context context, UUID id, ClassifyCommand command) {
        return mutate(context, id, command.expectedVersion(), exceptionCase -> {
            String before = exceptionCase.category() + "/" + exceptionCase.severity();
            OperationalExceptionCase.EscalationLevel beforeLevel = exceptionCase.escalationLevel();
            exceptionCase.classify(command.category(), command.severity(), command.reason(), now());
            append(exceptionCase, "CLASSIFIED", before,
                exceptionCase.category() + "/" + exceptionCase.severity(), command.reason(), context, now());
            if (exceptionCase.severity() == OperationalExceptionCase.Severity.CRITICAL
                    && beforeLevel == OperationalExceptionCase.EscalationLevel.L0
                    && "L1".equals(exceptionCase.escalationLevel().name())) {
                events.publishEscalation(exceptionCase, now());
            }
        });
    }

    @Override public CaseDetail acknowledge(Context context, UUID id, VersionCommand command) {
        return mutate(context, id, command.expectedVersion(), value -> transition(value, "ACKNOWLEDGED", context, value::acknowledge));
    }

    @Override
    public CaseDetail assign(Context context, UUID id, AssignCommand command) {
        return mutate(context, id, command.expectedVersion(), exceptionCase -> {
            validateAssignment(context, command);
            var beforeType = exceptionCase.assignmentType();
            UUID beforeUser = exceptionCase.assignedUserId();
            String beforeRole = exceptionCase.assignedRoleCode();
            exceptionCase.assign(command.assignmentType(), command.userId(), command.roleCode(), now());
            history.appendAssignment(new AssignmentHistory(UUID.randomUUID(), context.tenantId(), id,
                beforeType, beforeUser, beforeRole, exceptionCase.assignmentType(), exceptionCase.assignedUserId(),
                exceptionCase.assignedRoleCode(), context.actorId(), context.username(), requiredReason(command.reason()), now()));
            append(exceptionCase, "ASSIGNED", assignment(beforeType, beforeUser, beforeRole),
                assignment(exceptionCase.assignmentType(), exceptionCase.assignedUserId(), exceptionCase.assignedRoleCode()),
                command.reason(), context, now());
        });
    }

    @Override public CaseDetail start(Context context, UUID id, VersionCommand command) {
        return mutate(context, id, command.expectedVersion(), value -> transition(value, "STARTED", context, value::start));
    }

    @Override
    public CaseDetail escalate(Context context, UUID id, ReasonCommand command) {
        return mutate(context, id, command.expectedVersion(), exceptionCase -> {
            String before = exceptionCase.escalationLevel().name();
            exceptionCase.escalate(now());
            append(exceptionCase, "ESCALATED", before, exceptionCase.escalationLevel().name(),
                requiredReason(command.reason()), context, now());
            events.publishEscalation(exceptionCase, now());
        });
    }

    @Override
    public CaseDetail addCorrectiveAction(Context context, UUID id, CorrectiveActionCommand command) {
        return transactions.execute(() -> {
            OperationalExceptionCase exceptionCase = required(context, id);
            checkVersion(command.expectedVersion(), exceptionCase.version());
            validateOwner(context.tenantId(), command.ownerType(), command.ownerUserId(), command.ownerRoleCode());
            var action = CorrectiveAction.open(UUID.randomUUID(), context.tenantId(), id, command.type(),
                command.description(), command.ownerType(), command.ownerUserId(), command.ownerRoleCode(),
                command.dueAt(), command.evidenceReference(), now());
            actions.save(action);
            append(exceptionCase, "CORRECTIVE_ACTION_CREATED", null, action.id().toString(), null, context, now());
            return detail(exceptionCase);
        });
    }

    @Override
    public CaseDetail startCorrectiveAction(Context context, UUID id, UUID actionId, VersionCommand command) {
        return changeAction(context, id, actionId, command.expectedVersion(), "CORRECTIVE_ACTION_STARTED", CorrectiveAction::start);
    }

    @Override
    public CaseDetail completeCorrectiveAction(Context context, UUID id, UUID actionId, VersionCommand command) {
        return changeAction(context, id, actionId, command.expectedVersion(), "CORRECTIVE_ACTION_COMPLETED", CorrectiveAction::complete);
    }

    @Override
    public CaseDetail recordRca(Context context, UUID id, RcaCommand command) {
        return transactions.execute(() -> {
            OperationalExceptionCase exceptionCase = required(context, id);
            checkVersion(command.expectedVersion(), exceptionCase.version());
            if (rcas.findRcaByCase(context.tenantId(), id).isPresent()) {
                throw new ConflictException("OPERATIONAL_EXCEPTION_CONFLICT", "RCA already exists for this case");
            }
            RootCauseAnalysis rca = RootCauseAnalysis.create(UUID.randomUUID(), context.tenantId(), id,
                command.causeCategory(), command.rootCauseCode(), command.summary(), command.contributingFactors(),
                context.actorId(), now());
            rcas.save(rca);
            append(exceptionCase, "RCA_RECORDED", null, rca.rootCauseCode(), null, context, now());
            return detail(exceptionCase);
        });
    }

    @Override
    public CaseDetail approveRca(Context context, UUID id, RcaApprovalCommand command) {
        return transactions.execute(() -> {
            OperationalExceptionCase exceptionCase = required(context, id);
            checkVersion(command.expectedCaseVersion(), exceptionCase.version());
            RootCauseAnalysis rca = rca(context.tenantId(), id);
            checkVersion(command.expectedRcaVersion(), rca.version());
            rca.approve(context.actorId(), now());
            rcas.save(rca);
            append(exceptionCase, "RCA_APPROVED", null, rca.rootCauseCode(), null, context, now());
            return detail(exceptionCase);
        });
    }

    @Override
    public CaseDetail resolve(Context context, UUID id, ResolveCommand command) {
        return mutate(context, id, command.expectedVersion(), exceptionCase -> {
            requireActionsComplete(context.tenantId(), id);
            transition(exceptionCase, "RESOLVED", context,
                when -> exceptionCase.resolve(command.resolutionNote(), command.resultReference(), context.actorId(), when));
        });
    }

    @Override
    public CaseDetail close(Context context, UUID id, VersionCommand command) {
        return mutate(context, id, command.expectedVersion(), exceptionCase -> {
            requireActionsComplete(context.tenantId(), id);
            RootCauseAnalysis rca = rcas.findRcaByCase(context.tenantId(), id).orElse(null);
            boolean ready = rca != null && rca.approved();
            String before = exceptionCase.status().name();
            exceptionCase.close(context.actorId(), ready, now());
            append(exceptionCase, "CLOSED", before, exceptionCase.status().name(), null, context, now());
        });
    }

    @Override public CaseDetail rejectResolution(Context context, UUID id, ReasonCommand command) {
        return mutate(context, id, command.expectedVersion(), value -> {
            String before = value.status().name();
            value.rejectResolution(command.reason(), now());
            append(value, "RESOLUTION_REJECTED", before, value.status().name(), command.reason(), context, now());
        });
    }

    @Override public CaseDetail reopen(Context context, UUID id, ReasonCommand command) {
        return mutate(context, id, command.expectedVersion(), value -> {
            String before = value.status().name();
            value.reopen(command.reason(), now());
            append(value, "REOPENED", before, value.status().name(), command.reason(), context, now());
        });
    }

    @Override
    public int scanDue(UUID tenantId) {
        return transactions.execute(() -> {
            List<OperationalExceptionCase> due = cases.findDue(tenantId, now(), 50);
            int escalated = 0;
            for (OperationalExceptionCase exceptionCase : due) {
                if (exceptionCase.status() == OperationalExceptionCase.Status.RESOLVED
                        || exceptionCase.status() == OperationalExceptionCase.Status.CLOSED
                        || exceptionCase.escalationLevel() == OperationalExceptionCase.EscalationLevel.L3) continue;
                String before = exceptionCase.escalationLevel().name();
                exceptionCase.escalate(now());
                cases.save(exceptionCase);
                Context system = new Context(tenantId, SYSTEM_ACTOR, "system:operational-exception-sla", exceptionCase.correlationId());
                append(exceptionCase, "ESCALATED", before, exceptionCase.escalationLevel().name(),
                    exceptionCase.slaStatus(now()).name(), system, now());
                events.publishEscalation(exceptionCase, now());
                escalated++;
            }
            return escalated;
        });
    }

    private CaseDetail mutate(Context context, UUID id, long expectedVersion,
                              java.util.function.Consumer<OperationalExceptionCase> mutation) {
        return transactions.execute(() -> {
            OperationalExceptionCase value = required(context, id);
            checkVersion(expectedVersion, value.version());
            mutation.accept(value);
            return detail(cases.save(value));
        });
    }

    private CaseDetail changeAction(Context context, UUID caseId, UUID actionId, long expectedVersion,
                                    String historyAction, ActionChange change) {
        return transactions.execute(() -> {
            OperationalExceptionCase exceptionCase = required(context, caseId);
            CorrectiveAction action = actions.find(context.tenantId(), caseId, actionId)
                .orElseThrow(() -> notFound());
            checkVersion(expectedVersion, action.version());
            String before = action.status().name();
            change.apply(action, now());
            actions.save(action);
            append(exceptionCase, historyAction, before, action.status().name(), null, context, now());
            return detail(exceptionCase);
        });
    }

    private void transition(OperationalExceptionCase exceptionCase, String action, Context context, TimedChange change) {
        String before = exceptionCase.status().name();
        change.apply(now());
        append(exceptionCase, action, before, exceptionCase.status().name(), null, context, now());
    }

    private void validateAssignment(Context context, AssignCommand command) {
        validateOwner(context.tenantId(), command.assignmentType(), command.userId(), command.roleCode());
    }

    private void validateOwner(UUID tenantId, OperationalExceptionCase.AssignmentType type, UUID userId, String role) {
        if (type == null) return;
        boolean valid = type == OperationalExceptionCase.AssignmentType.USER
            ? userId != null && role == null && assignees.eligibleUser(tenantId, userId)
            : userId == null && role != null && assignees.activeRole(role);
        if (!valid) throw new BusinessRuleException("OPERATIONAL_EXCEPTION_ASSIGNMENT_INVALID", "Assignment target is not eligible");
    }

    private void requireActionsComplete(UUID tenantId, UUID caseId) {
        boolean incomplete = actions.findActionsByCase(tenantId, caseId).stream()
            .anyMatch(action -> action.status() != CorrectiveAction.Status.COMPLETED);
        if (incomplete) throw new BusinessRuleException("OPERATIONAL_EXCEPTION_CLOSE_NOT_ALLOWED",
            "All corrective actions must be completed");
    }

    private OperationalExceptionCase required(Context context, UUID id) {
        validateContext(context);
        return cases.find(context.tenantId(), id).orElseThrow(OperationalExceptionService::notFound);
    }

    private RootCauseAnalysis rca(UUID tenantId, UUID caseId) {
        return rcas.findRcaByCase(tenantId, caseId).orElseThrow(() ->
            new BusinessRuleException("OPERATIONAL_EXCEPTION_RCA_REQUIRED", "Root cause analysis is required"));
    }

    private CaseDetail detail(OperationalExceptionCase value) {
        return new CaseDetail(value, actions.findActionsByCase(value.tenantId(), value.id()),
            rcas.findRcaByCase(value.tenantId(), value.id()).orElse(null));
    }

    private void append(OperationalExceptionCase value, String action, String before, String after, String reason,
                        Context context, OffsetDateTime occurredAt) {
        history.append(new OperationalExceptionHistory(UUID.randomUUID(), value.tenantId(), value.id(), action,
            before, after, clean(reason), context.actorId(), context.username(), context.correlationId(),
            value.version(), occurredAt));
    }

    private Context system(OperationalExceptionFactV1 fact) {
        return new Context(fact.tenantId(), SYSTEM_ACTOR, "system:operations-intake", fact.correlationId());
    }

    private String nextReference(UUID tenantId) {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder value = new StringBuilder("OEX-");
            for (int index = 0; index < 12; index++) value.append(CROCKFORD.charAt(random.nextInt(CROCKFORD.length())));
            if (!cases.referenceExists(tenantId, value.toString())) return value.toString();
        }
        throw new ConflictException("OPERATIONAL_EXCEPTION_CONFLICT", "Unable to allocate a unique case reference");
    }

    private static String queue(OperationalExceptionCase.Category category) {
        return switch (category) {
            case SAFETY -> "OPERATIONS_SAFETY_QUEUE";
            case COMPLIANCE -> "OPERATIONS_COMPLIANCE_QUEUE";
            case CUSTOMER -> "OPERATIONS_CUSTOMER_QUEUE";
            case FINANCIAL -> "OPERATIONS_FINANCIAL_QUEUE";
            case TECHNICAL -> "OPERATIONS_TECHNICAL_QUEUE";
            case SECURITY -> "OPERATIONS_SECURITY_QUEUE";
            case OPERATIONAL -> "OPERATIONS_QUEUE";
        };
    }

    private static String assignment(OperationalExceptionCase.AssignmentType type, UUID userId, String role) {
        if (type == null) return null;
        return type == OperationalExceptionCase.AssignmentType.USER ? "USER:" + userId : "ROLE_QUEUE:" + role;
    }

    private static void checkVersion(long expected, long actual) {
        if (expected != actual) throw new ConflictException("OPERATIONAL_EXCEPTION_CONFLICT",
            "Operational exception state changed; reload and retry");
    }

    private static int bounded(int supplied, int defaultValue, int max) {
        int value = supplied <= 0 ? defaultValue : supplied;
        if (value > max) throw new BusinessRuleException("OPERATIONAL_EXCEPTION_SLA_INVALID", "Page size exceeds maximum " + max);
        return value;
    }

    private static String allowedSort(String supplied) {
        String value = clean(supplied);
        if (value == null) return "openedAt";
        return switch (value) {
            case "openedAt", "updatedAt", "severity", "responseDueAt", "resolutionDueAt", "status" -> value;
            default -> throw new BusinessRuleException("OPERATIONAL_EXCEPTION_SLA_INVALID", "Unsupported sort field");
        };
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String requiredReason(String value) {
        String result = clean(value);
        if (result == null || result.length() > 2000) throw new BusinessRuleException("OPERATIONAL_EXCEPTION_CLOSE_NOT_ALLOWED", "Reason is required");
        return result;
    }
    private static void validateContext(Context context) {
        if (context == null || context.tenantId() == null || context.actorId() == null
                || context.username() == null || context.username().isBlank()) {
            throw new BusinessRuleException("TENANT_REQUIRED", "Trusted Tenant and actor context is required");
        }
    }
    private static NotFoundException notFound() {
        return new NotFoundException("OPERATIONAL_EXCEPTION_NOT_FOUND", "Operational exception was not found");
    }
    private OffsetDateTime now() { return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }

    @FunctionalInterface private interface TimedChange { void apply(OffsetDateTime now); }
    @FunctionalInterface private interface ActionChange { void apply(CorrectiveAction action, OffsetDateTime now); }
}
