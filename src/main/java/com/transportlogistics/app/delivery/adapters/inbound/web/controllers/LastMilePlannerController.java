package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.LastMilePlannerContextResponse;
import com.transportlogistics.app.delivery.ports.inbound.LastMilePlannerUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/deliveries/{id}/last-mile-planner")
public class LastMilePlannerController {
    private final LastMilePlannerUseCase useCase;

    public LastMilePlannerController(LastMilePlannerUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('DELIVERY_FAIL_VIEW', 'DELIVERY_EXCEPTION_VIEW')")
    public ResponseEntity<LastMilePlannerContextResponse> getContext(@PathVariable("id") UUID deliveryId) {
        return ResponseEntity.ok(LastMilePlannerContextResponse.from(useCase.getContext(deliveryId)));
    }
}
