package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.*;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.*;
import com.transportlogistics.app.delivery.adapters.inbound.web.mappers.DeliveryOrderWebMapper;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1/deliveries")
public class DeliveryOrderController {
    private final DeliveryOrderUseCase orders;
    private final DeliveryOrderWebMapper mapper;
    public DeliveryOrderController(DeliveryOrderUseCase orders, DeliveryOrderWebMapper mapper) {
        this.orders = orders; this.mapper = mapper;
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public DeliveryOrderResponse create(@Valid @RequestBody CreateDeliveryOrderRequest request, Principal principal) {
        return mapper.toResponse(orders.create(mapper.toCommand(request), actor(principal)));
    }
    @GetMapping("/{id}") public DeliveryOrderResponse get(@PathVariable UUID id) { return mapper.toResponse(orders.get(id)); }
    @GetMapping public DeliveryOrderPageResponse search(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String search,
            @RequestParam(required = false) DeliveryStatus status, @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) OffsetDateTime windowFrom,
            @RequestParam(required = false) OffsetDateTime windowTo) {
        var result = orders.search(new DeliveryOrderUseCase.SearchQuery(search, status, customerId, windowFrom, windowTo, page, size));
        return new DeliveryOrderPageResponse(result.content().stream().map(mapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
    @PatchMapping("/{id}") public DeliveryOrderResponse update(@PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryOrderRequest request, Principal principal) {
        return mapper.toResponse(orders.update(id, mapper.toCommand(request), actor(principal)));
    }
    @PostMapping("/{id}/validate-readiness") public DeliveryOrderResponse markReady(@PathVariable UUID id,
            @Valid @RequestBody DeliveryVersionRequest request, Principal principal) {
        return mapper.toResponse(orders.markReady(id, request.version(), actor(principal)));
    }
    private String actor(Principal principal) { return PrincipalUtils.resolveActorName(principal, null); }
}
