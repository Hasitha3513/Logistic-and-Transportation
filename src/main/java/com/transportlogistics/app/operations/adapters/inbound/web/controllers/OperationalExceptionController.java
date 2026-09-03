package com.transportlogistics.app.operations.adapters.inbound.web.controllers;

import com.transportlogistics.app.operations.adapters.inbound.web.dto.request.OperationalExceptionRequests;
import com.transportlogistics.app.operations.adapters.inbound.web.dto.response.OperationalExceptionResponses;
import com.transportlogistics.app.operations.adapters.inbound.web.mappers.OperationalExceptionWebMapper;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;
import com.transportlogistics.app.shared.web.CorrelationIdFilter;
import com.transportlogistics.app.tenancy.CurrentTenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/v1/operational-exceptions")
public class OperationalExceptionController {
    private final OperationalExceptionUseCase operations;
    private final OperationalExceptionWebMapper mapper;
    private final CurrentTenant currentTenant;
    private final Clock clock;

    public OperationalExceptionController(OperationalExceptionUseCase operations, OperationalExceptionWebMapper mapper,
                                          CurrentTenant currentTenant, Clock clock) {
        this.operations = operations;
        this.mapper = mapper;
        this.currentTenant = currentTenant;
        this.clock = clock;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_VIEW')")
    public OperationalExceptionResponses.Page<OperationalExceptionResponses.Case> list(
            @RequestParam(required = false) OperationalExceptionCase.Status status,
            @RequestParam(required = false) OperationalExceptionCase.Severity severity,
            @RequestParam(required = false) OperationalExceptionCase.Category category,
            @RequestParam(required = false) OperationalExceptionCase.SourceModule sourceModule,
            @RequestParam(required = false) UUID assignedUserId,
            @RequestParam(required = false) String assignedRoleCode,
            @RequestParam(required = false) OperationalExceptionCase.SlaStatus slaStatus,
            @RequestParam(required = false) OffsetDateTime openedFrom,
            @RequestParam(required = false) OffsetDateTime openedTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "openedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        var result = operations.list(context(request), new OperationalExceptionUseCase.Query(status, severity,
            category, sourceModule, assignedUserId, assignedRoleCode, slaStatus, openedFrom, openedTo, search,
            sort, !"asc".equalsIgnoreCase(direction), page, size));
        return new OperationalExceptionResponses.Page<>(result.content().stream()
            .map(value -> mapper.toResponse(value, now())).toList(), result.page(), result.size(),
            result.totalElements(), result.totalPages());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_VIEW')")
    public OperationalExceptionResponses.Detail get(@PathVariable UUID id, HttpServletRequest request,
                                                     Authentication authentication) {
        return response(operations.get(context(request), id), authentication);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_AUDIT_VIEW')")
    public OperationalExceptionResponses.Page<OperationalExceptionResponses.History> history(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        var result = operations.history(context(request), id, page, size);
        return new OperationalExceptionResponses.Page<>(result.content().stream().map(mapper::toResponse).toList(),
            result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @PostMapping("/{id}/classify")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_MANAGE')")
    public OperationalExceptionResponses.Detail classify(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.Classify body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.classify(context(request), id, new OperationalExceptionUseCase.ClassifyCommand(
            body.expectedVersion(), body.category(), body.severity(), body.reason())), authentication);
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_MANAGE')")
    public OperationalExceptionResponses.Detail acknowledge(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.Version body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.acknowledge(context(request), id,
            new OperationalExceptionUseCase.VersionCommand(body.expectedVersion())), authentication);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyAuthority('OPERATIONAL_EXCEPTION_ASSIGN','OPERATIONAL_EXCEPTION_MANAGE')")
    public OperationalExceptionResponses.Detail assign(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.Assign body, HttpServletRequest request,
            Authentication authentication) {
        var context = context(request);
        boolean canAssign = has(authentication, "OPERATIONAL_EXCEPTION_ASSIGN");
        boolean selfAssignment = body.assignmentType() == OperationalExceptionCase.AssignmentType.USER
            && context.actorId().equals(body.userId());
        if (!canAssign && !selfAssignment) throw new AccessDeniedException("Self-assignment only");
        return response(operations.assign(context, id, new OperationalExceptionUseCase.AssignCommand(
            body.expectedVersion(), body.assignmentType(), body.userId(), body.roleCode(), body.reason())), authentication);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_MANAGE')")
    public OperationalExceptionResponses.Detail start(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.Version body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.start(context(request), id,
            new OperationalExceptionUseCase.VersionCommand(body.expectedVersion())), authentication);
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_ESCALATE')")
    public OperationalExceptionResponses.Detail escalate(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.Reason body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.escalate(context(request), id,
            new OperationalExceptionUseCase.ReasonCommand(body.expectedVersion(), body.reason())), authentication);
    }

    @PostMapping("/{id}/corrective-actions")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_MANAGE')")
    public OperationalExceptionResponses.Detail addAction(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.CorrectiveActionCreate body,
            HttpServletRequest request, Authentication authentication) {
        return response(operations.addCorrectiveAction(context(request), id,
            new OperationalExceptionUseCase.CorrectiveActionCommand(body.expectedVersion(), body.type(),
                body.description(), body.ownerType(), body.ownerUserId(), body.ownerRoleCode(), body.dueAt(),
                body.evidenceReference())), authentication);
    }

    @PostMapping("/{id}/corrective-actions/{actionId}/start")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_MANAGE')")
    public OperationalExceptionResponses.Detail startAction(@PathVariable UUID id, @PathVariable UUID actionId,
            @Valid @RequestBody OperationalExceptionRequests.Version body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.startCorrectiveAction(context(request), id, actionId,
            new OperationalExceptionUseCase.VersionCommand(body.expectedVersion())), authentication);
    }

    @PostMapping("/{id}/corrective-actions/{actionId}/complete")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_MANAGE')")
    public OperationalExceptionResponses.Detail completeAction(@PathVariable UUID id, @PathVariable UUID actionId,
            @Valid @RequestBody OperationalExceptionRequests.Version body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.completeCorrectiveAction(context(request), id, actionId,
            new OperationalExceptionUseCase.VersionCommand(body.expectedVersion())), authentication);
    }

    @PostMapping("/{id}/rca")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_RCA')")
    public OperationalExceptionResponses.Detail recordRca(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.Rca body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.recordRca(context(request), id, new OperationalExceptionUseCase.RcaCommand(
            body.expectedVersion(), body.causeCategory(), body.rootCauseCode(), body.summary(),
            body.contributingFactors())), authentication);
    }

    @PostMapping("/{id}/rca/approve")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_RCA')")
    public OperationalExceptionResponses.Detail approveRca(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.RcaApproval body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.approveRca(context(request), id,
            new OperationalExceptionUseCase.RcaApprovalCommand(body.expectedCaseVersion(), body.expectedRcaVersion())),
            authentication);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_MANAGE')")
    public OperationalExceptionResponses.Detail resolve(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.Resolve body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.resolve(context(request), id, new OperationalExceptionUseCase.ResolveCommand(
            body.expectedVersion(), body.resolutionNote(), body.resultReference())), authentication);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_CLOSE')")
    public OperationalExceptionResponses.Detail close(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.Version body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.close(context(request), id,
            new OperationalExceptionUseCase.VersionCommand(body.expectedVersion())), authentication);
    }

    @PostMapping("/{id}/reject-resolution")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_CLOSE')")
    public OperationalExceptionResponses.Detail reject(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.Reason body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.rejectResolution(context(request), id,
            new OperationalExceptionUseCase.ReasonCommand(body.expectedVersion(), body.reason())), authentication);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('OPERATIONAL_EXCEPTION_CLOSE')")
    public OperationalExceptionResponses.Detail reopen(@PathVariable UUID id,
            @Valid @RequestBody OperationalExceptionRequests.Reason body, HttpServletRequest request,
            Authentication authentication) {
        return response(operations.reopen(context(request), id,
            new OperationalExceptionUseCase.ReasonCommand(body.expectedVersion(), body.reason())), authentication);
    }

    private OperationalExceptionResponses.Detail response(OperationalExceptionUseCase.CaseDetail detail,
                                                            Authentication authentication) {
        return mapper.toResponse(detail, has(authentication, "OPERATIONAL_EXCEPTION_RCA"), now());
    }
    private OperationalExceptionUseCase.Context context(HttpServletRequest request) {
        var current = currentTenant.required();
        Object supplied = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return new OperationalExceptionUseCase.Context(current.tenantId(), current.actorId(), current.username(),
            supplied == null ? current.correlationId() : supplied.toString());
    }
    private static boolean has(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
            .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
    private OffsetDateTime now() { return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
}
