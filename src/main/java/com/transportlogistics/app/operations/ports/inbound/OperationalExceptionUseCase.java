package com.transportlogistics.app.operations.ports.inbound;

import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.operations.domain.model.CorrectiveAction;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionHistory;
import com.transportlogistics.app.operations.domain.model.RootCauseAnalysis;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OperationalExceptionUseCase {
    OperationalExceptionCase intake(OperationalExceptionFactV1 fact);
    PageResult<OperationalExceptionCase> list(Context context, Query query);
    CaseDetail get(Context context, UUID id);
    PageResult<OperationalExceptionHistory> history(Context context, UUID id, int page, int size);
    CaseDetail classify(Context context, UUID id, ClassifyCommand command);
    CaseDetail acknowledge(Context context, UUID id, VersionCommand command);
    CaseDetail assign(Context context, UUID id, AssignCommand command);
    CaseDetail start(Context context, UUID id, VersionCommand command);
    CaseDetail escalate(Context context, UUID id, ReasonCommand command);
    CaseDetail addCorrectiveAction(Context context, UUID id, CorrectiveActionCommand command);
    CaseDetail startCorrectiveAction(Context context, UUID id, UUID actionId, VersionCommand command);
    CaseDetail completeCorrectiveAction(Context context, UUID id, UUID actionId, VersionCommand command);
    CaseDetail recordRca(Context context, UUID id, RcaCommand command);
    CaseDetail approveRca(Context context, UUID id, RcaApprovalCommand command);
    CaseDetail resolve(Context context, UUID id, ResolveCommand command);
    CaseDetail close(Context context, UUID id, VersionCommand command);
    CaseDetail rejectResolution(Context context, UUID id, ReasonCommand command);
    CaseDetail reopen(Context context, UUID id, ReasonCommand command);
    int scanDue(UUID tenantId);

    record Context(UUID tenantId, UUID actorId, String username, String correlationId) {}
    record Query(OperationalExceptionCase.Status status, OperationalExceptionCase.Severity severity,
                 OperationalExceptionCase.Category category, OperationalExceptionCase.SourceModule sourceModule,
                 UUID assignedUserId, String assignedRoleCode, OperationalExceptionCase.SlaStatus slaStatus,
                 OffsetDateTime openedFrom, OffsetDateTime openedTo, String search,
                 String sort, boolean descending, int page, int size) {}
    record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
    record CaseDetail(OperationalExceptionCase exceptionCase, List<CorrectiveAction> correctiveActions,
                      RootCauseAnalysis rca) {}
    record VersionCommand(long expectedVersion) {}
    record ReasonCommand(long expectedVersion, String reason) {}
    record ClassifyCommand(long expectedVersion, OperationalExceptionCase.Category category,
                           OperationalExceptionCase.Severity severity, String reason) {}
    record AssignCommand(long expectedVersion, OperationalExceptionCase.AssignmentType assignmentType,
                         UUID userId, String roleCode, String reason) {}
    record CorrectiveActionCommand(long expectedVersion, CorrectiveAction.Type type, String description,
                                   OperationalExceptionCase.AssignmentType ownerType, UUID ownerUserId,
                                   String ownerRoleCode, OffsetDateTime dueAt, String evidenceReference) {}
    record RcaCommand(long expectedVersion, RootCauseAnalysis.CauseCategory causeCategory,
                      String rootCauseCode, String summary, String contributingFactors) {}
    record RcaApprovalCommand(long expectedCaseVersion, long expectedRcaVersion) {}
    record ResolveCommand(long expectedVersion, String resolutionNote, String resultReference) {}
}
