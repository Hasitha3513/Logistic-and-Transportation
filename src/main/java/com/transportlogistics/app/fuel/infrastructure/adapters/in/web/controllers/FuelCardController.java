package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fuel.application.ports.in.FuelCardUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelCard;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.FuelCardRequests;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelCardResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers.FuelCardWebMapper;
import com.transportlogistics.app.tenancy.CurrentTenant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/fuel/cards")
public class FuelCardController {
    private final FuelCardUseCase cards; private final CurrentTenant tenants; private final FuelCardWebMapper mapper;
    public FuelCardController(FuelCardUseCase cards, CurrentTenant tenants, FuelCardWebMapper mapper) {
        this.cards=cards; this.tenants=tenants; this.mapper=mapper;
    }
    @GetMapping public List<FuelCardResponse> list(@RequestParam(defaultValue="0") int page,
                                                   @RequestParam(defaultValue="20") int limit,
                                                   @RequestParam(required=false) FuelCard.Status status,
                                                   @RequestParam(required=false) UUID providerId,
                                                   @RequestParam(required=false) String bindingType,
                                                   @RequestParam(required=false) UUID bindingId,
                                                   @RequestParam(required=false) Integer expiryFrom,
                                                   @RequestParam(required=false) Integer expiryTo,
                                                   @RequestParam(required=false) Boolean reviewRequired,
                                                   @RequestParam(defaultValue="createdAt") String sort,
                                                   @RequestParam(defaultValue="desc") String direction) {
        var search = new FuelCardUseCase.Search(page, limit, status, providerId, bindingType, bindingId,
                expiryFrom, expiryTo, reviewRequired, sort, direction);
        return cards.list(context().tenantId(),search).stream().map(mapper::toResponse).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public FuelCardResponse create(@Valid @RequestBody FuelCardRequests.Create r) {
        return mapper.toResponse(cards.create(context(),new FuelCardUseCase.Create(r.providerId(),r.alias(),
                r.providerCardReference(),r.maskedIdentifier(),r.lastFour(),r.expiryMonth(),r.expiryYear())));
    }
    @GetMapping("/{id}") public FuelCardResponse get(@PathVariable UUID id) {
        return mapper.toResponse(cards.get(context().tenantId(),id));
    }
    @PutMapping("/{id}") public FuelCardResponse update(@PathVariable UUID id,
                                                         @Valid @RequestBody FuelCardRequests.Update r) {
        return mapper.toResponse(cards.update(context(), id,
                new FuelCardUseCase.Update(r.alias(), r.expiryMonth(), r.expiryYear(), r.version())));
    }
    @PostMapping("/{id}/activate") public FuelCardResponse activate(@PathVariable UUID id,@Valid @RequestBody FuelCardRequests.Command r){return transition(id,r,FuelCard.Status.ACTIVE);}
    @PostMapping("/{id}/suspend") public FuelCardResponse suspend(@PathVariable UUID id,@Valid @RequestBody FuelCardRequests.Command r){return transition(id,r,FuelCard.Status.SUSPENDED);}
    @PostMapping("/{id}/resume") public FuelCardResponse resume(@PathVariable UUID id,@Valid @RequestBody FuelCardRequests.Command r){return transition(id,r,FuelCard.Status.ACTIVE);}
    @PostMapping("/{id}/block") public FuelCardResponse block(@PathVariable UUID id,@Valid @RequestBody FuelCardRequests.Command r){return transition(id,r,FuelCard.Status.BLOCKED);}
    @PostMapping("/{id}/cancel") public FuelCardResponse cancel(@PathVariable UUID id,@Valid @RequestBody FuelCardRequests.Command r){return transition(id,r,FuelCard.Status.CANCELLED);}
    @PostMapping("/{id}/bindings") public FuelCardUseCase.Binding bind(@PathVariable UUID id,@Valid @RequestBody FuelCardRequests.Binding r){return cards.bind(context(),id,new FuelCardUseCase.Bind(r.bindingType(),r.bindingId(),r.version(),r.reason()));}
    @GetMapping("/{id}/bindings") public List<FuelCardUseCase.Binding> bindings(@PathVariable UUID id){return cards.bindings(context().tenantId(),id);}
    @GetMapping("/{id}/history") public List<FuelCardUseCase.History> history(@PathVariable UUID id,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int limit){return cards.history(context().tenantId(),id,page,limit);}
    @PutMapping("/{id}/restrictions") public FuelCardUseCase.Restriction restrictions(@PathVariable UUID id,@Valid @RequestBody FuelCardRequests.Restriction r){return cards.restrict(context(),id,new FuelCardUseCase.Restrict(r.currency(),r.maxTransactionAmount(),r.maxDailyAmount(),r.maxMonthlyAmount(),r.maxDailyLitres(),r.allowedFuelTypes(),r.allowedStationReferences(),r.version(),r.reason()));}
    private FuelCardResponse transition(UUID id,FuelCardRequests.Command r,FuelCard.Status s){return mapper.toResponse(cards.transition(context(),id,s,r.version(),r.reason()));}
    private FuelCardUseCase.Context context(){var c=tenants.required();return new FuelCardUseCase.Context(c.tenantId(),c.actorId());}
}
