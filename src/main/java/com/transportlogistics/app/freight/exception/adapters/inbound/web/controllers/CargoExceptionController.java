package com.transportlogistics.app.freight.exception.adapters.inbound.web.controllers;

import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.CreateCargoExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.EscalateExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.HoldExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.ReleaseExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.RejectExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request.ResolveExceptionRequest;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.response.CargoExceptionResponse;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.mappers.CargoExceptionWebMapper;
import com.transportlogistics.app.freight.exception.domain.ExceptionStatus;
import com.transportlogistics.app.freight.exception.domain.ExceptionType;
import com.transportlogistics.app.freight.exception.ports.inbound.CargoExceptionUseCase;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Cargo Exception lifecycle (US-30).
 * All business rules are enforced by the domain + application layer.
 * Controllers contain no business logic.
 */
@RestController
@RequestMapping("/v1/freight/exceptions")
public class CargoExceptionController {

    private final CargoExceptionUseCase useCase;
    private final CargoExceptionWebMapper mapper;

    public CargoExceptionController(CargoExceptionUseCase useCase,
                                    CargoExceptionWebMapper mapper) {
        this.useCase = useCase;
        this.mapper  = mapper;
    }

    // ── POST /v1/freight/exceptions ───────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CargoExceptionResponse record(@Valid @RequestBody CreateCargoExceptionRequest request,
                                         Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new CargoExceptionUseCase.RecordExceptionCommand(
                request.exceptionType(),
                request.severity(),
                request.freightOrderId(),
                request.manifestId(),
                request.manifestItemId(),
                request.description(),
                request.impact(),
                request.restriction(),
                request.correctiveAction()
        );
        return mapper.toResponse(useCase.record(command, actor));
    }

    // ── GET /v1/freight/exceptions ────────────────────────────────────────────

    @GetMapping
    public List<CargoExceptionResponse> list(
            @RequestParam(required = false) UUID freightOrderId,
            @RequestParam(required = false) UUID manifestId,
            @RequestParam(required = false) ExceptionType type,
            @RequestParam(required = false) ExceptionStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return useCase.list(freightOrderId, manifestId, type, status, page, size)
                      .stream().map(mapper::toResponse).toList();
    }

    // ── GET /v1/freight/exceptions/{id} ───────────────────────────────────────

    @GetMapping("/{id}")
    public CargoExceptionResponse get(@PathVariable UUID id) {
        return mapper.toResponse(useCase.get(id));
    }

    // ── POST /v1/freight/exceptions/{id}/hold ────────────────────────────────

    @PostMapping("/{id}/hold")
    public CargoExceptionResponse hold(@PathVariable UUID id,
                                       @Valid @RequestBody HoldExceptionRequest request,
                                       Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new CargoExceptionUseCase.HoldExceptionCommand(
                request.restriction(), request.reason(), request.version());
        return mapper.toResponse(useCase.hold(id, command, actor));
    }

    // ── POST /v1/freight/exceptions/{id}/escalate ─────────────────────────────

    @PostMapping("/{id}/escalate")
    public CargoExceptionResponse escalate(@PathVariable UUID id,
                                           @Valid @RequestBody EscalateExceptionRequest request,
                                           Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new CargoExceptionUseCase.EscalateExceptionCommand(
                request.reason(), request.version());
        return mapper.toResponse(useCase.escalate(id, command, actor));
    }

    // ── POST /v1/freight/exceptions/{id}/release ──────────────────────────────

    @PostMapping("/{id}/release")
    public CargoExceptionResponse release(@PathVariable UUID id,
                                          @Valid @RequestBody ReleaseExceptionRequest request,
                                          Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new CargoExceptionUseCase.ReleaseExceptionCommand(
                request.reason(), request.version());
        return mapper.toResponse(useCase.release(id, command, actor));
    }

    // ── POST /v1/freight/exceptions/{id}/reject ───────────────────────────────

    @PostMapping("/{id}/reject")
    public CargoExceptionResponse reject(@PathVariable UUID id,
                                         @Valid @RequestBody RejectExceptionRequest request,
                                         Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new CargoExceptionUseCase.RejectExceptionCommand(
                request.reason(), request.version());
        return mapper.toResponse(useCase.reject(id, command, actor));
    }

    // ── POST /v1/freight/exceptions/{id}/resolve ──────────────────────────────

    @PostMapping("/{id}/resolve")
    public CargoExceptionResponse resolve(@PathVariable UUID id,
                                          @Valid @RequestBody ResolveExceptionRequest request,
                                          Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new CargoExceptionUseCase.ResolveExceptionCommand(
                request.resolution(), request.correctiveAction(), request.reason(), request.version());
        return mapper.toResponse(useCase.resolve(id, command, actor));
    }
}
