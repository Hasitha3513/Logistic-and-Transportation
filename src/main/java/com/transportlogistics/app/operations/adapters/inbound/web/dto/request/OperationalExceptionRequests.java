package com.transportlogistics.app.operations.adapters.inbound.web.dto.request;

import com.transportlogistics.app.operations.domain.model.CorrectiveAction;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
import com.transportlogistics.app.operations.domain.model.RootCauseAnalysis;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class OperationalExceptionRequests {
    private OperationalExceptionRequests() {}

    public record Version(@PositiveOrZero long expectedVersion) {}

    public record Reason(@PositiveOrZero long expectedVersion,
                         @NotBlank @Size(max = 2000) String reason) {}

    public record Classify(@PositiveOrZero long expectedVersion,
                           @NotNull OperationalExceptionCase.Category category,
                           @NotNull OperationalExceptionCase.Severity severity,
                           @NotBlank @Size(max = 2000) String reason) {}

    public record Assign(@PositiveOrZero long expectedVersion,
                         OperationalExceptionCase.AssignmentType assignmentType,
                         UUID userId,
                         @Size(max = 80) String roleCode,
                         @NotBlank @Size(max = 2000) String reason) {}

    public record CorrectiveActionCreate(@PositiveOrZero long expectedVersion,
                                         @NotNull CorrectiveAction.Type type,
                                         @NotBlank @Size(max = 2000) String description,
                                         @NotNull OperationalExceptionCase.AssignmentType ownerType,
                                         UUID ownerUserId,
                                         @Size(max = 80) String ownerRoleCode,
                                         @FutureOrPresent OffsetDateTime dueAt,
                                         @Size(max = 160) String evidenceReference) {}

    public record Rca(@PositiveOrZero long expectedVersion,
                      @NotNull RootCauseAnalysis.CauseCategory causeCategory,
                      @NotBlank @Size(max = 80) String rootCauseCode,
                      @NotBlank @Size(max = 2000) String summary,
                      @Size(max = 2000) String contributingFactors) {}

    public record RcaApproval(@PositiveOrZero long expectedCaseVersion,
                              @PositiveOrZero long expectedRcaVersion) {}

    public record Resolve(@PositiveOrZero long expectedVersion,
                          @NotBlank @Size(max = 2000) String resolutionNote,
                          @Size(max = 160) String resultReference) {}
}
