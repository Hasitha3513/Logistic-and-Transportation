package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.controllers;

import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.request.CreateLoadPlanRequest;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.request.UpdateLoadPlanRequest;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response.LoadPlanResponse;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response.LoadPlanValidationResponse;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response.LoadValidationResultResponse;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.mappers.LoadPlanWebMapper;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanViolation;
import com.transportlogistics.app.freight.loadplanning.domain.LoadValidationResult;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.LoadPlanUseCase;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/freight/load-plans")
public class LoadPlanController {

    private final LoadPlanUseCase loadPlanUseCase;
    private final LoadPlanWebMapper mapper;

    public LoadPlanController(LoadPlanUseCase loadPlanUseCase, LoadPlanWebMapper mapper) {
        this.loadPlanUseCase = loadPlanUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoadPlanResponse create(@Valid @RequestBody CreateLoadPlanRequest request, Principal principal) {
        LoadPlan created = loadPlanUseCase.create(mapper.toCreateCommand(request), actor(principal));
        return mapper.toResponse(created);
    }

    @GetMapping("/{id}")
    public LoadPlanResponse get(@PathVariable UUID id) {
        LoadPlan loadPlan = loadPlanUseCase.get(id);
        return mapper.toResponse(loadPlan);
    }

    @GetMapping
    public List<LoadPlanResponse> list() {
        List<LoadPlan> loadPlans = loadPlanUseCase.list();
        return loadPlans.stream().map(mapper::toResponse).toList();
    }

    @PatchMapping("/{id}")
    public LoadPlanResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateLoadPlanRequest request,
                                   Principal principal) {
        LoadPlan updated = loadPlanUseCase.update(id, mapper.toUpdateCommand(request), actor(principal));
        return mapper.toResponse(updated);
    }

    @PostMapping("/{id}/validate-layout")
    public LoadPlanValidationResponse validateLayout(@PathVariable UUID id) {
        List<LoadPlanViolation> violations = loadPlanUseCase.validateLayout(id);
        return mapper.toValidationResponse(violations);
    }

    @PostMapping("/{id}/validate-weight-volume")
    public LoadValidationResultResponse validateWeightAndVolume(@PathVariable UUID id, Principal principal) {
        LoadValidationResult result = loadPlanUseCase.validateWeightAndVolume(id, actor(principal));
        return mapper.toValidationResultResponse(result);
    }

    private String actor(Principal principal) {
        return PrincipalUtils.resolveActorName(principal, null);
    }
}
