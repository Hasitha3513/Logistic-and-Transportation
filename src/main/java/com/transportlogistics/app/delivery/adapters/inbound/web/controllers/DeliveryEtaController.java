package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.BatchEtaResponse;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.BatchEtaStopResponse;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.SingleOrderEtaResponse;
import com.transportlogistics.app.delivery.domain.model.BatchEtaEstimate;
import com.transportlogistics.app.delivery.domain.model.SingleOrderEtaEstimate;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryEtaUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryEtaController {

    private final DeliveryEtaUseCase etaUseCase;

    public DeliveryEtaController(DeliveryEtaUseCase etaUseCase) {
        this.etaUseCase = etaUseCase;
    }

    @GetMapping("/orders/{orderId}/eta")
    @PreAuthorize("hasAuthority('DELIVERY_VIEW')")
    public ResponseEntity<SingleOrderEtaResponse> getOrderEta(@PathVariable UUID orderId) {
        SingleOrderEtaEstimate estimate = etaUseCase.getOrderEta(orderId);
        return ResponseEntity.ok(toOrderResponse(estimate));
    }

    @PostMapping("/orders/{orderId}/eta/calculate")
    @PreAuthorize("hasAuthority('DELIVERY_UPDATE')")
    public ResponseEntity<SingleOrderEtaResponse> calculateOrderEta(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        SingleOrderEtaEstimate estimate = etaUseCase.calculateOrderEta(orderId, actor);
        return ResponseEntity.ok(toOrderResponse(estimate));
    }

    @GetMapping("/batches/{batchId}/eta")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_VIEW')")
    public ResponseEntity<BatchEtaResponse> getBatchEta(@PathVariable UUID batchId) {
        BatchEtaEstimate estimate = etaUseCase.getBatchEta(batchId);
        return ResponseEntity.ok(toBatchResponse(estimate));
    }

    @PostMapping("/batches/{batchId}/eta/calculate")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_UPDATE')")
    public ResponseEntity<BatchEtaResponse> calculateBatchEta(
            @PathVariable UUID batchId,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        BatchEtaEstimate estimate = etaUseCase.calculateBatchEta(batchId, actor);
        return ResponseEntity.ok(toBatchResponse(estimate));
    }

    private SingleOrderEtaResponse toOrderResponse(SingleOrderEtaEstimate estimate) {
        boolean isStale = estimate.isStale(OffsetDateTime.now(ZoneOffset.UTC));
        return new SingleOrderEtaResponse(
                estimate.orderId(),
                estimate.estimatedArrivalAt(),
                estimate.travelDurationSeconds(),
                estimate.distanceMeters(),
                estimate.slaStatus(),
                estimate.source(),
                estimate.calculatedAt(),
                estimate.staleAt(),
                isStale
        );
    }

    private BatchEtaResponse toBatchResponse(BatchEtaEstimate estimate) {
        boolean isStale = estimate.isStale(OffsetDateTime.now(ZoneOffset.UTC));
        var stops = estimate.stops().stream()
                .map(s -> new BatchEtaStopResponse(
                        s.deliveryOrderId(),
                        s.sequence(),
                        s.estimatedArrivalAt(),
                        s.travelDurationSeconds(),
                        s.serviceDurationSeconds(),
                        s.distanceMeters(),
                        s.slaStatus()
                ))
                .toList();

        return new BatchEtaResponse(
                estimate.batchId(),
                estimate.calculatedAt(),
                estimate.staleAt(),
                estimate.totalDurationSeconds(),
                estimate.totalDistanceMeters(),
                estimate.estimatedCompletionAt(),
                estimate.source(),
                isStale,
                stops
        );
    }
}
