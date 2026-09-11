package com.transportlogistics.app.operations.adapters.outbound.persistence;

import com.transportlogistics.app.operations.domain.model.AssignmentHistory;
import com.transportlogistics.app.operations.domain.model.CorrectiveAction;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionHistory;
import com.transportlogistics.app.operations.domain.model.RootCauseAnalysis;
import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;
import com.transportlogistics.app.operations.ports.outbound.CorrectiveActionRepository;
import com.transportlogistics.app.operations.ports.outbound.OperationalExceptionCaseRepository;
import com.transportlogistics.app.operations.ports.outbound.OperationalExceptionHistoryRepository;
import com.transportlogistics.app.operations.ports.outbound.RootCauseAnalysisRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class OperationsPersistenceAdapter implements OperationalExceptionCaseRepository, CorrectiveActionRepository,
        RootCauseAnalysisRepository, OperationalExceptionHistoryRepository {
    private final OperationalExceptionCaseJpaRepository caseJpa;
    private final CorrectiveActionJpaRepository actionJpa;
    private final RootCauseAnalysisJpaRepository rcaJpa;
    private final OperationalExceptionHistoryJpaRepository historyJpa;
    private final AssignmentHistoryJpaRepository assignmentJpa;

    OperationsPersistenceAdapter(OperationalExceptionCaseJpaRepository caseJpa,
                                 CorrectiveActionJpaRepository actionJpa,
                                 RootCauseAnalysisJpaRepository rcaJpa,
                                 OperationalExceptionHistoryJpaRepository historyJpa,
                                 AssignmentHistoryJpaRepository assignmentJpa) {
        this.caseJpa = caseJpa;
        this.actionJpa = actionJpa;
        this.rcaJpa = rcaJpa;
        this.historyJpa = historyJpa;
        this.assignmentJpa = assignmentJpa;
    }

    @Override public OperationalExceptionCase save(OperationalExceptionCase value) {
        var existing = caseJpa.findByTenantIdAndId(value.tenantId(), value.id());
        var entity = existing.orElseGet(OperationalExceptionCaseEntity::new);
        return toDomain(caseJpa.saveAndFlush(toEntity(value, entity, existing.isEmpty())));
    }
    @Override public Optional<OperationalExceptionCase> find(UUID tenantId, UUID id) {
        return caseJpa.findByTenantIdAndId(tenantId, id).map(this::toDomain);
    }
    @Override public Optional<OperationalExceptionCase> findBySourceEvent(UUID tenantId, UUID sourceEventId) {
        return caseJpa.findByTenantIdAndSourceEventId(tenantId, sourceEventId).map(this::toDomain);
    }
    @Override public boolean referenceExists(UUID tenantId, String reference) {
        return caseJpa.existsByTenantIdAndCaseReference(tenantId, reference);
    }

    @Override
    public OperationalExceptionUseCase.PageResult<OperationalExceptionCase> search(UUID tenantId,
            OperationalExceptionUseCase.Query query, OffsetDateTime now) {
        Specification<OperationalExceptionCaseEntity> spec = (root, ignored, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));
            if (query.status() != null) predicates.add(builder.equal(root.get("status"), query.status()));
            if (query.severity() != null) predicates.add(builder.equal(root.get("severity"), query.severity()));
            if (query.category() != null) predicates.add(builder.equal(root.get("category"), query.category()));
            if (query.sourceModule() != null) predicates.add(builder.equal(root.get("sourceModule"), query.sourceModule()));
            if (query.assignedUserId() != null) predicates.add(builder.equal(root.get("assignedUserId"), query.assignedUserId()));
            if (query.assignedRoleCode() != null) predicates.add(builder.equal(root.get("assignedRoleCode"), query.assignedRoleCode()));
            if (query.openedFrom() != null) predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), query.openedFrom()));
            if (query.openedTo() != null) predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), query.openedTo()));
            addSearch(query.search(), root, builder, predicates);
            addSla(query.slaStatus(), now, root, builder, predicates);
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        String property = "openedAt".equals(query.sort()) ? "occurredAt" : query.sort();
        Sort sort = Sort.by(query.descending() ? Sort.Direction.DESC : Sort.Direction.ASC, property).and(Sort.by("id"));
        var page = caseJpa.findAll(spec, PageRequest.of(query.page(), query.size(), sort));
        return new OperationalExceptionUseCase.PageResult<>(page.stream().map(this::toDomain).toList(),
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static void addSearch(String search, jakarta.persistence.criteria.Root<OperationalExceptionCaseEntity> root,
                                  jakarta.persistence.criteria.CriteriaBuilder builder, List<Predicate> predicates) {
        if (search == null) return;
        try {
            UUID id = UUID.fromString(search);
            predicates.add(builder.equal(root.get("sourceId"), id));
        } catch (IllegalArgumentException ignored) {
            String normalized = search.toUpperCase(java.util.Locale.ROOT);
            predicates.add(builder.or(builder.like(root.get("caseReference"), normalized + "%"),
                builder.equal(root.get("summaryCode"), normalized)));
        }
    }

    private static void addSla(OperationalExceptionCase.SlaStatus sla, OffsetDateTime now,
                               jakarta.persistence.criteria.Root<OperationalExceptionCaseEntity> root,
                               jakarta.persistence.criteria.CriteriaBuilder builder, List<Predicate> predicates) {
        if (sla == null) return;
        switch (sla) {
            case MET -> predicates.add(builder.and(builder.isNotNull(root.get("resolvedAt")),
                builder.lessThanOrEqualTo(root.get("resolvedAt"), root.get("resolutionDueAt"))));
            case BREACHED -> predicates.add(builder.or(
                builder.greaterThan(root.get("resolvedAt"), root.get("resolutionDueAt")),
                builder.and(builder.isNull(root.get("resolvedAt")), builder.lessThanOrEqualTo(root.get("resolutionDueAt"), now))));
            case AT_RISK -> predicates.add(builder.and(builder.isNull(root.get("resolvedAt")),
                builder.lessThanOrEqualTo(root.get("nextEscalationAt"), now), builder.greaterThan(root.get("resolutionDueAt"), now)));
            case ON_TRACK -> predicates.add(builder.and(builder.isNull(root.get("resolvedAt")),
                builder.greaterThan(root.get("nextEscalationAt"), now)));
        }
    }

    @Override public List<OperationalExceptionCase> findDue(UUID tenantId, OffsetDateTime now, int limit) {
        return caseJpa.findDue(tenantId, now, PageRequest.of(0, Math.min(50, limit))).stream().map(this::toDomain).toList();
    }

    @Override public CorrectiveAction save(CorrectiveAction value) {
        var existing = actionJpa.findByTenantIdAndCaseIdAndId(value.tenantId(), value.caseId(), value.id());
        var entity = existing.orElseGet(CorrectiveActionEntity::new);
        return actionToDomain(actionJpa.saveAndFlush(actionToEntity(value, entity, existing.isEmpty())));
    }
    @Override public Optional<CorrectiveAction> find(UUID tenantId, UUID caseId, UUID actionId) {
        return actionJpa.findByTenantIdAndCaseIdAndId(tenantId, caseId, actionId).map(this::actionToDomain);
    }
    @Override public List<CorrectiveAction> findActionsByCase(UUID tenantId, UUID caseId) {
        return actionJpa.findByTenantIdAndCaseIdOrderByCreatedAt(tenantId, caseId).stream().map(this::actionToDomain).toList();
    }

    @Override public RootCauseAnalysis save(RootCauseAnalysis value) {
        var existing = rcaJpa.findByTenantIdAndCaseId(value.tenantId(), value.caseId());
        var entity = existing.orElseGet(RootCauseAnalysisEntity::new);
        return rcaToDomain(rcaJpa.saveAndFlush(rcaToEntity(value, entity, existing.isEmpty())));
    }
    @Override public Optional<RootCauseAnalysis> findRcaByCase(UUID tenantId, UUID caseId) {
        return rcaJpa.findByTenantIdAndCaseId(tenantId, caseId).map(this::rcaToDomain);
    }

    @Override public void append(OperationalExceptionHistory value) { historyJpa.save(historyToEntity(value)); }
    @Override public void appendAssignment(AssignmentHistory value) { assignmentJpa.save(assignmentToEntity(value)); }
    @Override
    public OperationalExceptionUseCase.PageResult<OperationalExceptionHistory> findByCase(
            UUID tenantId, UUID caseId, int page, int size) {
        var result = historyJpa.findByTenantIdAndCaseId(tenantId, caseId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt", "id")));
        return new OperationalExceptionUseCase.PageResult<>(result.stream().map(this::historyToDomain).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private OperationalExceptionCaseEntity toEntity(OperationalExceptionCase source,
                                                      OperationalExceptionCaseEntity target, boolean isNew) {
        target.setId(source.id()); target.setTenantId(source.tenantId()); target.setCaseReference(source.caseReference());
        target.setSourceEventId(source.sourceEventId()); target.setSourceModule(source.sourceModule());
        target.setSourceType(source.sourceType()); target.setSourceId(source.sourceId()); target.setOccurredAt(source.occurredAt());
        target.setSummaryCode(source.summaryCode()); target.setCorrelationId(source.correlationId());
        target.setCategory(source.category()); target.setSeverity(source.severity()); target.setStatus(source.status());
        target.setResponseDueAt(source.responseDueAt()); target.setResolutionDueAt(source.resolutionDueAt());
        target.setNextEscalationAt(source.nextEscalationAt()); target.setAcknowledgedAt(source.acknowledgedAt());
        target.setResolvedAt(source.resolvedAt()); target.setClosedAt(source.closedAt());
        target.setAssignmentType(source.assignmentType()); target.setAssignedUserId(source.assignedUserId());
        target.setAssignedRoleCode(source.assignedRoleCode()); target.setEscalationLevel(source.escalationLevel());
        target.setResolutionNote(source.resolutionNote()); target.setResolutionResultReference(source.resolutionResultReference());
        target.setResolvedBy(source.resolvedBy()); target.setClosedBy(source.closedBy());
        target.setResolutionValidated(source.resolutionValidated());
        if (isNew) target.setVersion(source.version());
        target.setCreatedAt(source.createdAt()); target.setUpdatedAt(source.updatedAt());
        return target;
    }

    private OperationalExceptionCase toDomain(OperationalExceptionCaseEntity source) {
        return new OperationalExceptionCase(source.getId(), source.getTenantId(), source.getCaseReference(),
            source.getSourceEventId(), source.getSourceModule(), source.getSourceType(), source.getSourceId(),
            source.getOccurredAt(), source.getSummaryCode(), source.getCorrelationId(), source.getCategory(),
            source.getSeverity(), source.getStatus(), source.getResponseDueAt(), source.getResolutionDueAt(),
            source.getNextEscalationAt(), source.getAcknowledgedAt(), source.getResolvedAt(), source.getClosedAt(),
            source.getAssignmentType(), source.getAssignedUserId(), source.getAssignedRoleCode(),
            source.getEscalationLevel(), source.getResolutionNote(), source.getResolutionResultReference(),
            source.getResolvedBy(), source.getClosedBy(), source.isResolutionValidated(), source.getVersion(),
            source.getCreatedAt(), source.getUpdatedAt());
    }

    private CorrectiveActionEntity actionToEntity(CorrectiveAction source, CorrectiveActionEntity target,
                                                   boolean isNew) {
        target.setId(source.id()); target.setTenantId(source.tenantId()); target.setCaseId(source.caseId());
        target.setType(source.type()); target.setDescription(source.description()); target.setOwnerType(source.ownerType());
        target.setOwnerUserId(source.ownerUserId()); target.setOwnerRoleCode(source.ownerRoleCode()); target.setDueAt(source.dueAt());
        target.setStatus(source.status()); target.setCompletedAt(source.completedAt()); target.setEvidenceReference(source.evidenceReference());
        target.setCancellationReason(source.cancellationReason());
        if (isNew) target.setVersion(source.version());
        target.setCreatedAt(source.createdAt()); target.setUpdatedAt(source.updatedAt()); return target;
    }
    private CorrectiveAction actionToDomain(CorrectiveActionEntity source) {
        return new CorrectiveAction(source.getId(), source.getTenantId(), source.getCaseId(), source.getType(),
            source.getDescription(), source.getOwnerType(), source.getOwnerUserId(), source.getOwnerRoleCode(),
            source.getDueAt(), source.getStatus(), source.getCompletedAt(), source.getEvidenceReference(),
            source.getCancellationReason(), source.getVersion(), source.getCreatedAt(), source.getUpdatedAt());
    }
    private RootCauseAnalysisEntity rcaToEntity(RootCauseAnalysis source, RootCauseAnalysisEntity target,
                                                 boolean isNew) {
        target.setId(source.id()); target.setTenantId(source.tenantId()); target.setCaseId(source.caseId());
        target.setCauseCategory(source.causeCategory()); target.setRootCauseCode(source.rootCauseCode());
        target.setSummary(source.summary()); target.setContributingFactors(source.contributingFactors());
        target.setAuthorId(source.authorId()); target.setApproverId(source.approverId()); target.setApprovedAt(source.approvedAt());
        if (isNew) target.setVersion(source.version());
        target.setCreatedAt(source.createdAt()); target.setUpdatedAt(source.updatedAt()); return target;
    }
    private RootCauseAnalysis rcaToDomain(RootCauseAnalysisEntity source) {
        return new RootCauseAnalysis(source.getId(), source.getTenantId(), source.getCaseId(), source.getCauseCategory(),
            source.getRootCauseCode(), source.getSummary(), source.getContributingFactors(), source.getAuthorId(),
            source.getApproverId(), source.getApprovedAt(), source.getVersion(), source.getCreatedAt(), source.getUpdatedAt());
    }
    private OperationalExceptionHistoryEntity historyToEntity(OperationalExceptionHistory source) {
        var target = new OperationalExceptionHistoryEntity();
        target.setId(source.id()); target.setTenantId(source.tenantId()); target.setCaseId(source.caseId());
        target.setAction(source.action()); target.setBeforeValue(source.beforeValue()); target.setAfterValue(source.afterValue());
        target.setReason(source.reason()); target.setActorId(source.actorId()); target.setActorUsername(source.actorUsername());
        target.setCorrelationId(source.correlationId()); target.setResultingVersion(source.resultingVersion());
        target.setOccurredAt(source.occurredAt()); return target;
    }
    private OperationalExceptionHistory historyToDomain(OperationalExceptionHistoryEntity source) {
        return new OperationalExceptionHistory(source.getId(), source.getTenantId(), source.getCaseId(), source.getAction(),
            source.getBeforeValue(), source.getAfterValue(), source.getReason(), source.getActorId(), source.getActorUsername(),
            source.getCorrelationId(), source.getResultingVersion(), source.getOccurredAt());
    }
    private AssignmentHistoryEntity assignmentToEntity(AssignmentHistory source) {
        var target = new AssignmentHistoryEntity();
        target.setId(source.id()); target.setTenantId(source.tenantId()); target.setCaseId(source.caseId());
        target.setFromType(source.fromType()); target.setFromUserId(source.fromUserId()); target.setFromRoleCode(source.fromRoleCode());
        target.setToType(source.toType()); target.setToUserId(source.toUserId()); target.setToRoleCode(source.toRoleCode());
        target.setActorId(source.actorId()); target.setActorUsername(source.actorUsername()); target.setReason(source.reason());
        target.setOccurredAt(source.occurredAt()); return target;
    }
}
