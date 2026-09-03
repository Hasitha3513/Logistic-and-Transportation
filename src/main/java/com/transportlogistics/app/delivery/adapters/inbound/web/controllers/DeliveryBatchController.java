package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.AddOrdersToBatchRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.AssignRiderToBatchRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.AutoClusterBatchesRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.CreateDeliveryBatchRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.UpdateDeliveryBatchRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliveryBatchOrderResponse;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliveryBatchResponse;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatch;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchStatus;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryBatchUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries/batches")
@Tag(name = "Delivery Batches", description = "Delivery batching, clustering, and rider assignment endpoints")
public class DeliveryBatchController {

    private final DeliveryBatchUseCase batchUseCase;

    public DeliveryBatchController(DeliveryBatchUseCase batchUseCase) {
        this.batchUseCase = batchUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_CREATE')")
    @Operation(summary = "Create manual delivery batch")
    public ResponseEntity<DeliveryBatchResponse> createBatch(@Valid @RequestBody CreateDeliveryBatchRequest request) {
        DeliveryBatch created = batchUseCase.createBatch(new DeliveryBatchUseCase.CreateDeliveryBatchCommand(
                request.deliveryZoneId(),
                request.deliverySlotId(),
                request.maxBatchSize(),
                request.deliveryOrderIds(),
                request.riderId()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PostMapping("/auto-cluster")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_CREATE')")
    @Operation(summary = "Auto-cluster ready orders into batches")
    public ResponseEntity<List<DeliveryBatchResponse>> autoCluster(@Valid @RequestBody AutoClusterBatchesRequest request) {
        List<DeliveryBatch> batches = batchUseCase.autoClusterBatches(new DeliveryBatchUseCase.AutoClusterBatchesCommand(
                request.deliveryZoneId(),
                request.deliverySlotId(),
                request.maxBatchSize()
        ));
        return ResponseEntity.ok(batches.stream().map(this::toResponse).toList());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_VIEW')")
    @Operation(summary = "List delivery batches")
    public ResponseEntity<Map<String, Object>> listBatches(
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) UUID slotId,
            @RequestParam(required = false) UUID riderId,
            @RequestParam(required = false) DeliveryBatchStatus status,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        List<DeliveryBatch> items = batchUseCase.listBatches(zoneId, slotId, riderId, status, limit, offset);
        long total = batchUseCase.countBatches(zoneId, slotId, riderId, status);
        return ResponseEntity.ok(Map.of(
                "items", items.stream().map(this::toResponse).toList(),
                "total", total,
                "limit", limit,
                "offset", offset
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_VIEW')")
    @Operation(summary = "Get delivery batch details")
    public ResponseEntity<DeliveryBatchResponse> getBatch(@PathVariable UUID id) {
        DeliveryBatch batch = batchUseCase.getBatch(id);
        return ResponseEntity.ok(toResponse(batch));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_UPDATE')")
    @Operation(summary = "Update delivery batch metadata")
    public ResponseEntity<DeliveryBatchResponse> updateBatch(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryBatchRequest request
    ) {
        DeliveryBatch updated = batchUseCase.updateBatch(id, new DeliveryBatchUseCase.UpdateDeliveryBatchCommand(
                request.maxBatchSize()
        ));
        return ResponseEntity.ok(toResponse(updated));
    }

    @PostMapping("/{id}/ready")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_UPDATE')")
    @Operation(summary = "Mark batch as READY for assignment")
    public ResponseEntity<DeliveryBatchResponse> markReady(@PathVariable UUID id) {
        DeliveryBatch ready = batchUseCase.markReady(id);
        return ResponseEntity.ok(toResponse(ready));
    }

    @PostMapping("/{id}/orders")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_UPDATE')")
    @Operation(summary = "Add delivery orders to batch")
    public ResponseEntity<DeliveryBatchResponse> addOrders(
            @PathVariable UUID id,
            @Valid @RequestBody AddOrdersToBatchRequest request
    ) {
        DeliveryBatch updated = batchUseCase.addOrdersToBatch(id, new DeliveryBatchUseCase.AddOrdersToBatchCommand(
                request.deliveryOrderIds()
        ));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}/orders/{orderId}")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_UPDATE')")
    @Operation(summary = "Remove delivery order from batch")
    public ResponseEntity<DeliveryBatchResponse> removeOrder(
            @PathVariable UUID id,
            @PathVariable UUID orderId
    ) {
        DeliveryBatch updated = batchUseCase.removeOrderFromBatch(id, orderId);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PostMapping("/{id}/assign-rider")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_ASSIGN')")
    @Operation(summary = "Assign rider to delivery batch")
    public ResponseEntity<DeliveryBatchResponse> assignRider(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRiderToBatchRequest request
    ) {
        DeliveryBatch assigned = batchUseCase.assignRider(id, new DeliveryBatchUseCase.AssignRiderToBatchCommand(
                request.riderId(),
                request.isOverride(),
                request.overrideReason()
        ));
        return ResponseEntity.ok(toResponse(assigned));
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_DISPATCH')")
    @Operation(summary = "Dispatch delivery batch")
    public ResponseEntity<DeliveryBatchResponse> dispatchBatch(@PathVariable UUID id) {
        DeliveryBatch dispatched = batchUseCase.dispatchBatch(id);
        return ResponseEntity.ok(toResponse(dispatched));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_CANCEL')")
    @Operation(summary = "Cancel / disband delivery batch")
    public ResponseEntity<DeliveryBatchResponse> cancelBatch(@PathVariable UUID id) {
        DeliveryBatch cancelled = batchUseCase.cancelBatch(id);
        return ResponseEntity.ok(toResponse(cancelled));
    }

    @GetMapping("/{id}/orders")
    @PreAuthorize("hasAuthority('DELIVERY_BATCH_VIEW')")
    @Operation(summary = "Get delivery batch order memberships")
    public ResponseEntity<List<DeliveryBatchOrderResponse>> getBatchOrders(@PathVariable UUID id) {
        List<DeliveryBatchOrder> orders = batchUseCase.getBatchOrderMemberships(id);
        return ResponseEntity.ok(orders.stream().map(this::toOrderResponse).toList());
    }

    private DeliveryBatchResponse toResponse(DeliveryBatch batch) {
        return new DeliveryBatchResponse(
                batch.id(),
                batch.batchCode().value(),
                batch.deliveryZoneId(),
                batch.deliverySlotId(),
                batch.riderId(),
                batch.status().name(),
                batch.maxBatchSize(),
                batch.version(),
                batch.createdAt(),
                batch.updatedAt(),
                batch.createdBy(),
                batch.updatedBy()
        );
    }

    private DeliveryBatchOrderResponse toOrderResponse(DeliveryBatchOrder order) {
        return new DeliveryBatchOrderResponse(
                order.id(),
                order.batchId(),
                order.deliveryOrderId(),
                order.sequenceHint(),
                order.status().name(),
                order.addedAt(),
                order.addedBy(),
                order.removedAt(),
                order.removedBy()
        );
    }
}
