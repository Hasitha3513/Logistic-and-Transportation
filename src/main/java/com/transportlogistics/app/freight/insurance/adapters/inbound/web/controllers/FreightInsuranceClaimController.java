package com.transportlogistics.app.freight.insurance.adapters.inbound.web.controllers;

import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request.ApproveClaimRequest;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request.AssessClaimRequest;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request.CreateClaimRequest;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request.DisputeClaimRequest;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request.RecordSettlementRequest;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request.RejectClaimRequest;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.response.FreightInsuranceClaimResponse;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.mappers.FreightInsuranceWebMapper;
import com.transportlogistics.app.freight.insurance.domain.FreightInsuranceClaim;
import com.transportlogistics.app.freight.insurance.ports.inbound.FreightInsuranceUseCase;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/v1/freight/insurance/claims")
public class FreightInsuranceClaimController {

    private final FreightInsuranceUseCase useCase;
    private final FreightInsuranceWebMapper mapper;

    public FreightInsuranceClaimController(FreightInsuranceUseCase useCase, FreightInsuranceWebMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FreightInsuranceClaimResponse createClaim(@Valid @RequestBody CreateClaimRequest request,
                                                     Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new FreightInsuranceUseCase.CreateClaimCommand(
                request.policyId(),
                request.incidentReference(),
                request.damageDescription(),
                request.claimedAmount()
        );
        FreightInsuranceClaim claim = useCase.createClaim(command, actor);
        return mapper.toResponse(claim);
    }

    @GetMapping
    public List<FreightInsuranceClaimResponse> listClaims() {
        return useCase.listClaims().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public FreightInsuranceClaimResponse getClaim(@PathVariable UUID id) {
        return mapper.toResponse(useCase.getClaim(id));
    }

    @PostMapping("/{id}/assess")
    public FreightInsuranceClaimResponse assessClaim(@PathVariable UUID id,
                                                     @Valid @RequestBody AssessClaimRequest request,
                                                     Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new FreightInsuranceUseCase.AssessClaimCommand(
                request.assessedAmount(),
                request.assessmentNotes(),
                request.version()
        );
        FreightInsuranceClaim assessed = useCase.assessClaim(id, command, actor);
        return mapper.toResponse(assessed);
    }

    @PostMapping("/{id}/approve")
    public FreightInsuranceClaimResponse approveClaim(@PathVariable UUID id,
                                                      @Valid @RequestBody ApproveClaimRequest request,
                                                      Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new FreightInsuranceUseCase.ApproveClaimCommand(request.version());
        FreightInsuranceClaim approved = useCase.approveClaim(id, command, actor);
        return mapper.toResponse(approved);
    }

    @PostMapping("/{id}/reject")
    public FreightInsuranceClaimResponse rejectClaim(@PathVariable UUID id,
                                                     @Valid @RequestBody RejectClaimRequest request,
                                                     Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new FreightInsuranceUseCase.RejectClaimCommand(request.reason(), request.version());
        FreightInsuranceClaim rejected = useCase.rejectClaim(id, command, actor);
        return mapper.toResponse(rejected);
    }

    @PostMapping("/{id}/dispute")
    public FreightInsuranceClaimResponse disputeClaim(@PathVariable UUID id,
                                                      @Valid @RequestBody DisputeClaimRequest request,
                                                      Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new FreightInsuranceUseCase.DisputeClaimCommand(request.reason(), request.version());
        FreightInsuranceClaim disputed = useCase.disputeClaim(id, command, actor);
        return mapper.toResponse(disputed);
    }

    @PostMapping("/{id}/settlements")
    @ResponseStatus(HttpStatus.CREATED)
    public FreightInsuranceClaimResponse recordSettlement(@PathVariable UUID id,
                                                          @Valid @RequestBody RecordSettlementRequest request,
                                                          Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new FreightInsuranceUseCase.RecordSettlementCommand(
                request.amount(),
                request.currency(),
                request.settlementReference(),
                request.notes(),
                request.version()
        );
        FreightInsuranceClaim settled = useCase.recordSettlement(id, command, actor);
        return mapper.toResponse(settled);
    }
}
