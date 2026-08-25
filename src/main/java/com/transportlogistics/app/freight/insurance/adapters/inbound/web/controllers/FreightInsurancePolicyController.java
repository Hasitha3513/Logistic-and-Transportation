package com.transportlogistics.app.freight.insurance.adapters.inbound.web.controllers;

import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request.AssociatePolicyRequest;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request.UpdatePolicyRequest;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.response.FreightInsurancePolicyResponse;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.mappers.FreightInsuranceWebMapper;
import com.transportlogistics.app.freight.insurance.domain.FreightInsurancePolicy;
import com.transportlogistics.app.freight.insurance.ports.inbound.FreightInsuranceUseCase;
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
@RequestMapping("/v1/freight/insurance/policies")
public class FreightInsurancePolicyController {

    private final FreightInsuranceUseCase useCase;
    private final FreightInsuranceWebMapper mapper;

    public FreightInsurancePolicyController(FreightInsuranceUseCase useCase, FreightInsuranceWebMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FreightInsurancePolicyResponse associatePolicy(@Valid @RequestBody AssociatePolicyRequest request,
                                                         Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new FreightInsuranceUseCase.AssociatePolicyCommand(
                request.freightOrderId(),
                request.cargoManifestId(),
                request.insuranceProvider(),
                request.policyType(),
                request.coverageAmount(),
                request.premiumAmount(),
                request.currency(),
                request.validFrom(),
                request.validUntil()
        );
        FreightInsurancePolicy policy = useCase.associatePolicy(command, actor);
        return mapper.toResponse(policy);
    }

    @GetMapping
    public List<FreightInsurancePolicyResponse> listPolicies() {
        return useCase.listPolicies().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public FreightInsurancePolicyResponse getPolicy(@PathVariable UUID id) {
        return mapper.toResponse(useCase.getPolicy(id));
    }

    @PatchMapping("/{id}")
    public FreightInsurancePolicyResponse updatePolicy(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdatePolicyRequest request,
                                                       Principal principal) {
        String actor = PrincipalUtils.resolveActorName(principal, "anonymous");
        var command = new FreightInsuranceUseCase.UpdatePolicyCommand(
                request.insuranceProvider(),
                request.policyType(),
                request.coverageAmount(),
                request.premiumAmount(),
                request.validFrom(),
                request.validUntil(),
                request.status(),
                request.version()
        );
        FreightInsurancePolicy updated = useCase.updatePolicy(id, command, actor);
        return mapper.toResponse(updated);
    }
}
