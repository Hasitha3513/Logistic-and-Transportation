package com.transportlogistics.app.operations.adapters.inbound.web.mappers;

import com.transportlogistics.app.operations.adapters.inbound.web.dto.response.OperationalExceptionResponses;
import com.transportlogistics.app.operations.domain.model.CorrectiveAction;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionHistory;
import com.transportlogistics.app.operations.domain.model.RootCauseAnalysis;
import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;
import org.mapstruct.Mapper;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface OperationalExceptionWebMapper {
    default OperationalExceptionResponses.Case toResponse(OperationalExceptionCase source, OffsetDateTime now) {
        return new OperationalExceptionResponses.Case(source.id(), source.caseReference(), source.sourceModule().name(),
            source.sourceType(), source.sourceId(), source.occurredAt(), source.summaryCode(), source.category().name(),
            source.severity().name(), source.status().name(), source.slaStatus(now).name(), source.responseDueAt(),
            source.resolutionDueAt(), source.nextEscalationAt(), source.acknowledgedAt(), source.resolvedAt(),
            source.closedAt(), source.assignmentType() == null ? null : source.assignmentType().name(),
            source.assignedUserId(), source.assignedRoleCode(), source.escalationLevel().name(),
            source.resolutionNote(), source.resolutionResultReference(), source.version(), source.createdAt(), source.updatedAt());
    }

    default OperationalExceptionResponses.CorrectiveAction toResponse(CorrectiveAction source) {
        return new OperationalExceptionResponses.CorrectiveAction(source.id(), source.type().name(),
            source.description(), source.ownerType().name(), source.ownerUserId(), source.ownerRoleCode(),
            source.dueAt(), source.status().name(), source.completedAt(), source.evidenceReference(),
            source.version(), source.createdAt(), source.updatedAt());
    }

    default OperationalExceptionResponses.Rca toResponse(RootCauseAnalysis source) {
        if (source == null) return null;
        return new OperationalExceptionResponses.Rca(source.id(), source.causeCategory().name(),
            source.rootCauseCode(), source.summary(), source.contributingFactors(), source.authorId(),
            source.approverId(), source.approvedAt(), source.version());
    }

    default OperationalExceptionResponses.Detail toResponse(OperationalExceptionUseCase.CaseDetail source,
                                                              boolean includeRca, OffsetDateTime now) {
        return new OperationalExceptionResponses.Detail(toResponse(source.exceptionCase(), now),
            source.correctiveActions().stream().map(this::toResponse).toList(),
            includeRca ? toResponse(source.rca()) : null);
    }

    default OperationalExceptionResponses.History toResponse(OperationalExceptionHistory source) {
        return new OperationalExceptionResponses.History(source.id(), source.action(), source.beforeValue(),
            source.afterValue(), source.reason(), source.actorId(), source.actorUsername(), source.correlationId(),
            source.resultingVersion(), source.occurredAt());
    }
}
