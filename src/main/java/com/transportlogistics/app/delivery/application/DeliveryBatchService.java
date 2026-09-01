package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.events.DeliveryBatchCreatedEvent;
import com.transportlogistics.app.delivery.domain.events.DeliveryBatchOrderMembershipEvent;
import com.transportlogistics.app.delivery.domain.events.DeliveryBatchRiderAssignedEvent;
import com.transportlogistics.app.delivery.domain.events.DeliveryBatchStatusChangedEvent;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatch;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchCode;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryPriority;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryBatchUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchCodeGenerator;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchEventPublisherPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DriverEligibilityPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DeliveryBatchService implements DeliveryBatchUseCase {

    private final DeliveryBatchRepository batchRepository;
    private final DeliveryOrderRepository orderRepository;
    private final DeliveryRiderRepository riderRepository;
    private final DriverEligibilityPort driverEligibilityPort;
    private final DeliveryZoneLookupPort zoneLookupPort;
    private final DeliveryTenantContextPort tenantContext;
    private final DeliveryOrderTransaction transactions;
    private final DeliveryBatchCodeGenerator codeGenerator;
    private final DeliveryBatchEventPublisherPort eventPublisher;
    private final Clock clock;

    public DeliveryBatchService(
            DeliveryBatchRepository batchRepository,
            DeliveryOrderRepository orderRepository,
            DeliveryRiderRepository riderRepository,
            DriverEligibilityPort driverEligibilityPort,
            DeliveryZoneLookupPort zoneLookupPort,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            DeliveryBatchCodeGenerator codeGenerator,
            DeliveryBatchEventPublisherPort eventPublisher,
            Clock clock
    ) {
        this.batchRepository = batchRepository;
        this.orderRepository = orderRepository;
        this.riderRepository = riderRepository;
        this.driverEligibilityPort = driverEligibilityPort;
        this.zoneLookupPort = zoneLookupPort;
        this.tenantContext = tenantContext;
        this.transactions = transactions;
        this.codeGenerator = codeGenerator;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    private UUID requireTenantId() {
        return tenantContext.currentTenantId()
                .orElseThrow(() -> new BusinessRuleException("TENANT_REQUIRED", "Authoritative tenant context is required"));
    }

    @Override
    public DeliveryBatch createBatch(CreateDeliveryBatchCommand command) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String actor = "system";

        if (command.deliveryZoneId() == null) {
            throw new BusinessRuleException("DELIVERY_BATCH_ZONE_REQUIRED", "Delivery zone is required for batch creation");
        }

        return transactions.execute(() -> {
            // 1. Validate zone existence and status
            DeliveryZone zone = zoneLookupPort.findZone(command.deliveryZoneId())
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ZONE_NOT_FOUND", "Delivery zone not found: " + command.deliveryZoneId()));
            if (zone.status() != DeliveryZoneStatus.ACTIVE) {
                throw new ConflictException("DELIVERY_ZONE_INACTIVE", "Delivery zone is not active");
            }

            // 2. Generate batch code
            Year year = Year.from(now);
            DeliveryBatchCode batchCode = codeGenerator.next(tenantId, year);

            int maxSize = (command.maxBatchSize() != null && command.maxBatchSize() > 0) ? command.maxBatchSize() : 5;
            List<UUID> orderIds = command.deliveryOrderIds() != null ? command.deliveryOrderIds() : List.of();

            if (orderIds.size() > maxSize) {
                throw new ConflictException("DELIVERY_BATCH_CAPACITY_EXCEEDED", "Selected order count exceeds batch maximum size of " + maxSize);
            }

            // 3. Create Batch
            DeliveryBatch batch = DeliveryBatch.create(
                    UUID.randomUUID(),
                    tenantId,
                    batchCode,
                    command.deliveryZoneId(),
                    command.deliverySlotId(),
                    maxSize,
                    now,
                    actor
            );

            DeliveryBatch savedBatch = batchRepository.save(batch);

            // 4. Validate and attach orders atomically
            if (!orderIds.isEmpty()) {
                attachOrdersToBatch(savedBatch, orderIds, now, actor);
            }

            // 5. Optional rider assignment
            if (command.riderId() != null) {
                savedBatch = assignRiderInternal(savedBatch, command.riderId(), false, null, now, actor);
            }

            // Publish created event
            if (eventPublisher != null) {
                eventPublisher.publish(new DeliveryBatchCreatedEvent(
                        UUID.randomUUID(),
                        tenantId,
                        savedBatch.id(),
                        savedBatch.batchCode().value(),
                        savedBatch.deliveryZoneId(),
                        savedBatch.deliverySlotId(),
                        now,
                        actor
                ));
            }

            return savedBatch;
        });
    }

    @Override
    public List<DeliveryBatch> autoClusterBatches(AutoClusterBatchesCommand command) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String actor = "system";

        if (command.deliveryZoneId() == null) {
            throw new BusinessRuleException("DELIVERY_BATCH_ZONE_REQUIRED", "Delivery zone is required for auto clustering");
        }

        return transactions.execute(() -> {
            DeliveryZone zone = zoneLookupPort.findZone(command.deliveryZoneId())
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ZONE_NOT_FOUND", "Delivery zone not found: " + command.deliveryZoneId()));
            if (zone.status() != DeliveryZoneStatus.ACTIVE) {
                throw new ConflictException("DELIVERY_ZONE_INACTIVE", "Delivery zone is not active");
            }

            // Search ready orders for the tenant
            var searchResult = orderRepository.search(new com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase.SearchQuery(
                    null,
                    DeliveryStatus.READY_FOR_ASSIGNMENT,
                    null,
                    null,
                    null,
                    0,
                    1000
            ));

            List<DeliveryOrder> candidateOrders = searchResult.content().stream()
                    .filter(order -> {
                        var resolvedZone = zoneLookupPort.resolveZoneForLocation(order.destinationLocationId());
                        return resolvedZone.isPresent() && resolvedZone.get().id().equals(command.deliveryZoneId());
                    })
                    .toList();

            if (candidateOrders.isEmpty()) {
                return List.of();
            }

            // Filter out already actively batched orders
            List<UUID> candidateIds = candidateOrders.stream().map(o -> o.id().value()).toList();
            List<UUID> activeBatchedIds = batchRepository.findActiveBatchedDeliveryOrderIds(tenantId, candidateIds);

            List<DeliveryOrder> unbatchedOrders = candidateOrders.stream()
                    .filter(o -> !activeBatchedIds.contains(o.id().value()))
                    .sorted(Comparator
                            .comparing(DeliveryOrder::priority, Comparator.comparingInt(DeliveryPriority::ordinal).reversed())
                            .thenComparing(DeliveryOrder::createdAt))
                    .toList();

            if (unbatchedOrders.isEmpty()) {
                return List.of();
            }

            int batchSize = (command.maxBatchSize() != null && command.maxBatchSize() > 0) ? command.maxBatchSize() : 5;
            List<DeliveryBatch> createdBatches = new ArrayList<>();

            // Partition orders
            for (int i = 0; i < unbatchedOrders.size(); i += batchSize) {
                List<DeliveryOrder> chunk = unbatchedOrders.subList(i, Math.min(i + batchSize, unbatchedOrders.size()));
                List<UUID> chunkIds = chunk.stream().map(o -> o.id().value()).toList();

                Year year = Year.from(now);
                DeliveryBatchCode batchCode = codeGenerator.next(tenantId, year);

                DeliveryBatch batch = DeliveryBatch.create(
                        UUID.randomUUID(),
                        tenantId,
                        batchCode,
                        command.deliveryZoneId(),
                        command.deliverySlotId(),
                        batchSize,
                        now,
                        actor
                );

                DeliveryBatch savedBatch = batchRepository.save(batch);
                attachOrdersToBatch(savedBatch, chunkIds, now, actor);
                createdBatches.add(savedBatch);

                if (eventPublisher != null) {
                    eventPublisher.publish(new DeliveryBatchCreatedEvent(
                            UUID.randomUUID(),
                            tenantId,
                            savedBatch.id(),
                            savedBatch.batchCode().value(),
                            savedBatch.deliveryZoneId(),
                            savedBatch.deliverySlotId(),
                            now,
                            actor
                    ));
                }
            }

            return createdBatches;
        });
    }

    @Override
    public DeliveryBatch getBatch(UUID batchId) {
        UUID tenantId = requireTenantId();
        return batchRepository.findById(tenantId, batchId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_BATCH_NOT_FOUND", "Delivery batch not found: " + batchId));
    }

    @Override
    public List<DeliveryBatch> listBatches(UUID zoneId, UUID slotId, UUID riderId, DeliveryBatchStatus status, int limit, int offset) {
        UUID tenantId = requireTenantId();
        return batchRepository.findBatches(tenantId, zoneId, slotId, riderId, status, limit, offset);
    }

    @Override
    public long countBatches(UUID zoneId, UUID slotId, UUID riderId, DeliveryBatchStatus status) {
        UUID tenantId = requireTenantId();
        return batchRepository.countBatches(tenantId, zoneId, slotId, riderId, status);
    }

    @Override
    public DeliveryBatch updateBatch(UUID batchId, UpdateDeliveryBatchCommand command) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String actor = "system";

        return transactions.execute(() -> {
            DeliveryBatch batch = batchRepository.findByIdForUpdate(tenantId, batchId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_BATCH_NOT_FOUND", "Delivery batch not found: " + batchId));

            int newMaxSize = command.maxBatchSize() != null && command.maxBatchSize() > 0 ? command.maxBatchSize() : batch.maxBatchSize();
            int currentOrderCount = batchRepository.countActiveOrdersByBatchId(tenantId, batchId);
            if (currentOrderCount > newMaxSize) {
                throw new ConflictException("DELIVERY_BATCH_CAPACITY_EXCEEDED", "Current order count (" + currentOrderCount + ") exceeds requested max size of " + newMaxSize);
            }

            DeliveryBatch updated = batch.updateMetadata(newMaxSize, now, actor);
            return batchRepository.save(updated);
        });
    }

    @Override
    public DeliveryBatch markReady(UUID batchId) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String actor = "system";

        return transactions.execute(() -> {
            DeliveryBatch batch = batchRepository.findByIdForUpdate(tenantId, batchId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_BATCH_NOT_FOUND", "Delivery batch not found: " + batchId));

            int activeCount = batchRepository.countActiveOrdersByBatchId(tenantId, batchId);
            DeliveryBatch readyBatch = batch.markReady(activeCount, now, actor);
            DeliveryBatch saved = batchRepository.save(readyBatch);

            if (eventPublisher != null) {
                eventPublisher.publish(new DeliveryBatchStatusChangedEvent(
                        UUID.randomUUID(),
                        tenantId,
                        saved.id(),
                        batch.status().name(),
                        saved.status().name(),
                        now,
                        actor
                ));
            }

            return saved;
        });
    }

    @Override
    public DeliveryBatch addOrdersToBatch(UUID batchId, AddOrdersToBatchCommand command) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String actor = "system";

        if (command.deliveryOrderIds() == null || command.deliveryOrderIds().isEmpty()) {
            throw new BusinessRuleException("DELIVERY_BATCH_ORDERS_REQUIRED", "At least one order ID is required");
        }

        return transactions.execute(() -> {
            DeliveryBatch batch = batchRepository.findByIdForUpdate(tenantId, batchId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_BATCH_NOT_FOUND", "Delivery batch not found: " + batchId));

            if (batch.status() != DeliveryBatchStatus.DRAFT && batch.status() != DeliveryBatchStatus.READY) {
                throw new ConflictException("DELIVERY_BATCH_INVALID_STATE", "Orders can only be added to DRAFT or READY batches");
            }

            int currentCount = batchRepository.countActiveOrdersByBatchId(tenantId, batchId);
            if (currentCount + command.deliveryOrderIds().size() > batch.maxBatchSize()) {
                throw new ConflictException("DELIVERY_BATCH_CAPACITY_EXCEEDED", "Adding orders would exceed batch capacity of " + batch.maxBatchSize());
            }

            attachOrdersToBatch(batch, command.deliveryOrderIds(), now, actor);
            return batch;
        });
    }

    @Override
    public DeliveryBatch removeOrderFromBatch(UUID batchId, UUID deliveryOrderId) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String actor = "system";

        return transactions.execute(() -> {
            DeliveryBatch batch = batchRepository.findByIdForUpdate(tenantId, batchId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_BATCH_NOT_FOUND", "Delivery batch not found: " + batchId));

            if (batch.status() != DeliveryBatchStatus.DRAFT && batch.status() != DeliveryBatchStatus.READY) {
                throw new ConflictException("DELIVERY_BATCH_INVALID_STATE", "Orders can only be removed from DRAFT or READY batches");
            }

            DeliveryBatchOrder membership = batchRepository.findActiveMembershipByDeliveryOrderId(tenantId, deliveryOrderId)
                    .orElseThrow(() -> new NotFoundException("BATCH_ORDER_MEMBERSHIP_NOT_FOUND", "No active membership found for order: " + deliveryOrderId));

            if (!membership.batchId().equals(batchId)) {
                throw new ConflictException("BATCH_ORDER_MISMATCH", "Order does not belong to this batch");
            }

            DeliveryBatchOrder removed = membership.markRemoved(now, actor);
            batchRepository.saveOrderMembership(removed);

            if (eventPublisher != null) {
                eventPublisher.publish(new DeliveryBatchOrderMembershipEvent(
                        UUID.randomUUID(),
                        tenantId,
                        batchId,
                        deliveryOrderId,
                        "REMOVED",
                        now,
                        actor
                ));
            }

            return batch;
        });
    }

    @Override
    public DeliveryBatch assignRider(UUID batchId, AssignRiderToBatchCommand command) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String actor = "system";

        if (command.riderId() == null) {
            throw new BusinessRuleException("INVALID_RIDER_ID", "Rider ID is required");
        }

        return transactions.execute(() -> {
            DeliveryBatch batch = batchRepository.findByIdForUpdate(tenantId, batchId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_BATCH_NOT_FOUND", "Delivery batch not found: " + batchId));

            return assignRiderInternal(batch, command.riderId(), command.override(), command.overrideReason(), now, actor);
        });
    }

    @Override
    public DeliveryBatch dispatchBatch(UUID batchId) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String actor = "system";

        return transactions.execute(() -> {
            DeliveryBatch batch = batchRepository.findByIdForUpdate(tenantId, batchId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_BATCH_NOT_FOUND", "Delivery batch not found: " + batchId));

            DeliveryBatch dispatched = batch.dispatch(now, actor);
            DeliveryBatch saved = batchRepository.save(dispatched);

            if (eventPublisher != null) {
                eventPublisher.publish(new DeliveryBatchStatusChangedEvent(
                        UUID.randomUUID(),
                        tenantId,
                        saved.id(),
                        batch.status().name(),
                        saved.status().name(),
                        now,
                        actor
                ));
            }

            return saved;
        });
    }

    @Override
    public DeliveryBatch cancelBatch(UUID batchId) {
        UUID tenantId = requireTenantId();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String actor = "system";

        return transactions.execute(() -> {
            DeliveryBatch batch = batchRepository.findByIdForUpdate(tenantId, batchId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_BATCH_NOT_FOUND", "Delivery batch not found: " + batchId));

            DeliveryBatch cancelled = batch.cancel(now, actor);
            DeliveryBatch saved = batchRepository.save(cancelled);

            // Mark all active memberships removed
            List<DeliveryBatchOrder> memberships = batchRepository.findActiveOrderMembershipsByBatchId(tenantId, batchId);
            for (DeliveryBatchOrder m : memberships) {
                batchRepository.saveOrderMembership(m.markRemoved(now, actor));
            }

            if (eventPublisher != null) {
                eventPublisher.publish(new DeliveryBatchStatusChangedEvent(
                        UUID.randomUUID(),
                        tenantId,
                        saved.id(),
                        batch.status().name(),
                        saved.status().name(),
                        now,
                        actor
                ));
            }

            return saved;
        });
    }

    @Override
    public List<DeliveryBatchOrder> getBatchOrderMemberships(UUID batchId) {
        UUID tenantId = requireTenantId();
        return batchRepository.findAllOrderMembershipsByBatchId(tenantId, batchId);
    }

    private void attachOrdersToBatch(DeliveryBatch batch, List<UUID> orderIds, OffsetDateTime now, String actor) {
        UUID tenantId = batch.tenantId();

        // Sort order IDs deterministically to prevent deadlocks
        List<UUID> sortedOrderIds = orderIds.stream().distinct().sorted().toList();

        // Check if any order is already in an active batch
        List<UUID> alreadyBatched = batchRepository.findActiveBatchedDeliveryOrderIds(tenantId, sortedOrderIds);
        if (!alreadyBatched.isEmpty()) {
            throw new ConflictException("DELIVERY_BATCH_ORDER_ALREADY_BATCHED", "Order(s) already in an active batch: " + alreadyBatched);
        }

        int seq = 1;
        for (UUID orderId : sortedOrderIds) {
            DeliveryOrder order = orderRepository.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ORDER_NOT_FOUND", "Delivery order not found: " + orderId));

            if (order.status() != DeliveryStatus.READY_FOR_ASSIGNMENT && order.status() != DeliveryStatus.FAILED_ATTEMPT) {
                throw new ConflictException("DELIVERY_ORDER_NOT_READY", "Delivery order " + orderId + " is not ready (status=" + order.status() + ")");
            }

            // Validate zone match
            var orderZone = zoneLookupPort.resolveZoneForLocation(order.destinationLocationId());
            if (orderZone.isEmpty() || !orderZone.get().id().equals(batch.deliveryZoneId())) {
                throw new ConflictException("DELIVERY_BATCH_ZONE_MISMATCH", "Order destination zone does not match batch zone: " + batch.deliveryZoneId());
            }

            DeliveryBatchOrder batchOrder = DeliveryBatchOrder.create(
                    UUID.randomUUID(),
                    tenantId,
                    batch.id(),
                    orderId,
                    seq++,
                    now,
                    actor
            );
            batchRepository.saveOrderMembership(batchOrder);

            if (eventPublisher != null) {
                eventPublisher.publish(new DeliveryBatchOrderMembershipEvent(
                        UUID.randomUUID(),
                        tenantId,
                        batch.id(),
                        orderId,
                        "ADDED",
                        now,
                        actor
                ));
            }
        }
    }

    private DeliveryBatch assignRiderInternal(
            DeliveryBatch batch,
            UUID riderId,
            boolean isOverride,
            String overrideReason,
            OffsetDateTime now,
            String actor
    ) {
        UUID tenantId = batch.tenantId();

        // Lock rider
        DeliveryRider rider = riderRepository.findByIdForUpdate(riderId, tenantId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_RIDER_NOT_FOUND", "Delivery rider not found: " + riderId));

        if (rider.getStatus() != DeliveryRiderStatus.ACTIVE) {
            throw new ConflictException("DELIVERY_RIDER_INACTIVE", "Delivery rider is not active: " + riderId);
        }

        // Validate driver eligibility
        var driver = driverEligibilityPort.findDriver(rider.getDriverId())
                .orElseThrow(() -> new NotFoundException("DRIVER_NOT_FOUND", "Driver profile not found: " + rider.getDriverId()));
        if (!driver.active() || !"AVAILABLE".equalsIgnoreCase(driver.status())) {
            throw new ConflictException("DELIVERY_RIDER_DRIVER_NOT_ELIGIBLE", "Underlying driver is not active or available");
        }

        // Validate zone eligibility
        boolean zoneEligible = rider.getPrimaryZoneId().equals(batch.deliveryZoneId())
                || rider.getSecondaryZoneIds().contains(batch.deliveryZoneId());
        if (!zoneEligible && !isOverride) {
            throw new ConflictException("DELIVERY_RIDER_ZONE_MISMATCH", "Rider is not authorized for batch zone: " + batch.deliveryZoneId());
        }

        // Validate workload capacity
        int activeOrderCount = batchRepository.countActiveOrdersByBatchId(tenantId, batch.id());
        int currentRiderWorkload = riderRepository.countActiveAssignmentsForRider(riderId, tenantId);
        if (currentRiderWorkload + activeOrderCount > rider.getMaxConcurrentDeliveries() && !isOverride) {
            throw new ConflictException("DELIVERY_RIDER_CAPACITY_EXCEEDED", "Rider workload would exceed capacity (" + rider.getMaxConcurrentDeliveries() + ")");
        }

        // Assign rider to all member orders
        List<DeliveryBatchOrder> memberships = batchRepository.findActiveOrderMembershipsByBatchId(tenantId, batch.id());
        for (DeliveryBatchOrder membership : memberships) {
            Optional<DeliveryOrderRiderAssignment> activeAssignment = riderRepository.findActiveAssignmentForOrder(membership.deliveryOrderId(), tenantId);
            if (activeAssignment.isPresent()) {
                DeliveryOrderRiderAssignment current = activeAssignment.get();
                if (!current.getRiderId().equals(riderId)) {
                    current.reassign(actor, now);
                    riderRepository.saveAssignment(current);

                    DeliveryOrderRiderAssignment newAssignment = DeliveryOrderRiderAssignment.create(
                            UUID.randomUUID(),
                            tenantId,
                            membership.deliveryOrderId(),
                            riderId,
                            isOverride,
                            overrideReason,
                            actor,
                            now
                    );
                    riderRepository.saveAssignment(newAssignment);
                }
            } else {
                DeliveryOrderRiderAssignment newAssignment = DeliveryOrderRiderAssignment.create(
                        UUID.randomUUID(),
                        tenantId,
                        membership.deliveryOrderId(),
                        riderId,
                        isOverride,
                        overrideReason,
                        actor,
                        now
                );
                riderRepository.saveAssignment(newAssignment);
            }
        }

        DeliveryBatch assignedBatch = batch.assignRider(riderId, now, actor);
        DeliveryBatch saved = batchRepository.save(assignedBatch);

        if (eventPublisher != null) {
            eventPublisher.publish(new DeliveryBatchRiderAssignedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    saved.id(),
                    riderId,
                    memberships.size(),
                    now,
                    actor
            ));
        }

        return saved;
    }
}
