package com.transportlogistics.app.freight.order.adapters.inbound.web.controllers;

import com.transportlogistics.app.freight.order.adapters.inbound.web.dto.request.CreateFreightOrderRequest;
import com.transportlogistics.app.freight.order.adapters.inbound.web.dto.request.UpdateFreightOrderRequest;
import com.transportlogistics.app.freight.order.adapters.inbound.web.dto.response.FreightOrderPageResponse;
import com.transportlogistics.app.freight.order.adapters.inbound.web.dto.response.FreightOrderResponse;
import com.transportlogistics.app.freight.order.adapters.inbound.web.mappers.FreightOrderWebMapper;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1/freight/orders")
public class FreightOrderController {
    private final FreightOrderUseCase orders;
    private final FreightOrderWebMapper mapper;
    public FreightOrderController(FreightOrderUseCase orders, FreightOrderWebMapper mapper) { this.orders = orders; this.mapper = mapper; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FreightOrderResponse create(@Valid @RequestBody CreateFreightOrderRequest request, Principal principal) {
        return mapper.toResponse(orders.create(mapper.toCommand(request), actor(principal)));
    }

    @GetMapping("/{id}")
    public FreightOrderResponse get(@PathVariable UUID id) { return mapper.toResponse(orders.get(id)); }

    @GetMapping
    public FreightOrderPageResponse search(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int limit,
                                           @RequestParam(required = false) String search,
                                           @RequestParam(required = false) UUID customerId,
                                           @RequestParam(required = false) OffsetDateTime pickupFrom,
                                           @RequestParam(required = false) OffsetDateTime pickupTo,
                                           @RequestParam(defaultValue = "requestedPickupAt") String sort,
                                           @RequestParam(defaultValue = "desc") String direction) {
        var result = orders.search(new FreightOrderUseCase.SearchQuery(search, customerId, pickupFrom, pickupTo,
                page, limit, sort, direction));
        return new FreightOrderPageResponse(result.content().stream().map(mapper::toResponse).toList(), result.page(),
                result.limit(), result.totalElements(), result.totalPages());
    }

    @PatchMapping("/{id}")
    public FreightOrderResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateFreightOrderRequest request,
                                       Principal principal) {
        return mapper.toResponse(orders.update(id, mapper.toCommand(request), actor(principal)));
    }

    private String actor(Principal principal) { return PrincipalUtils.resolveActorName(principal, null); }
}
