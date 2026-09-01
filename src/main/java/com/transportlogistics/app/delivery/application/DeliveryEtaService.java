package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.events.DeliveryEtaCalculatedEvent;
import com.transportlogistics.app.delivery.domain.model.BatchEtaEstimate;
import com.transportlogistics.app.delivery.domain.model.BatchEtaStopEstimate;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatch;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
import com.transportlogistics.app.delivery.domain.model.EtaSource;
import com.transportlogistics.app.delivery.domain.model.EtaStatus;
import com.transportlogistics.app.delivery.domain.model.SingleOrderEtaEstimate;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryEtaUseCase;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryEtaEventPublisherPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.RiderEtaContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryZoneRepository;
import com.transportlogistics.app.delivery.ports.outbound.EtaCachePort;
import com.transportlogistics.app.delivery.ports.outbound.LastMileRoutingPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DeliveryEtaService implements DeliveryEtaUseCase {

    private static final long FRESHNESS_TTL_MINUTES = 15L;
    private static final long DOORSTEP_SERVICE_BUFFER_SECONDS = 300L; // 5 mins
    private static final long APARTMENT_SERVICE_BUFFER_SECONDS = 600L; // 10 mins

    private final DeliveryOrderRepository orderRepository;
    private final DeliveryBatchRepository batchRepository;
    private final DeliveryZoneRepository zoneRepository;
    private final RiderEtaContextPort riderEtaContextPort;
    private final DeliveryLocationLookupPort locationLookupPort;
    private final LastMileRoutingPort routingPort;
    private final EtaCachePort cachePort;
    private final DeliveryEtaEventPublisherPort eventPublisherPort;
    private final DeliveryTenantContextPort tenantContextPort;

    public DeliveryEtaService(
            DeliveryOrderRepository orderRepository,
            DeliveryBatchRepository batchRepository,
            DeliveryZoneRepository zoneRepository,
            RiderEtaContextPort riderEtaContextPort,
            DeliveryLocationLookupPort locationLookupPort,
            LastMileRoutingPort routingPort,
            EtaCachePort cachePort,
            DeliveryEtaEventPublisherPort eventPublisherPort,
            DeliveryTenantContextPort tenantContextPort
    ) {
        this.orderRepository = orderRepository;
        this.batchRepository = batchRepository;
        this.zoneRepository = zoneRepository;
        this.riderEtaContextPort = riderEtaContextPort;
        this.locationLookupPort = locationLookupPort;
        this.routingPort = routingPort;
        this.cachePort = cachePort;
        this.eventPublisherPort = eventPublisherPort;
        this.tenantContextPort = tenantContextPort;
    }

    private UUID requireCurrentTenantId() {
        return tenantContextPort.currentTenantId()
                .orElseThrow(() -> new BusinessRuleException("TENANT_REQUIRED", "Tenant context is required"));
    }

    @Override
    public SingleOrderEtaEstimate getOrderEta(UUID orderId) {
        UUID tenantId = requireCurrentTenantId();
        DeliveryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_ETA_SUBJECT_NOT_FOUND", "Delivery order not found: " + orderId));

        String fingerprint = buildOrderFingerprint(tenantId, order);
        Optional<SingleOrderEtaEstimate> cached = cachePort.getOrderEta(tenantId, orderId, fingerprint);
        if (cached.isPresent() && !cached.get().isStale(OffsetDateTime.now(ZoneOffset.UTC))) {
            return cached.get();
        }

        long generation = cachePort.beginOrderCalculation(tenantId, orderId);
        return doCalculateOrderEta(tenantId, order, fingerprint, generation, "system");
    }

    @Override
    public SingleOrderEtaEstimate calculateOrderEta(UUID orderId, String actor) {
        UUID tenantId = requireCurrentTenantId();
        DeliveryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_ETA_SUBJECT_NOT_FOUND", "Delivery order not found: " + orderId));

        String fingerprint = buildOrderFingerprint(tenantId, order);
        long generation = cachePort.beginOrderCalculation(tenantId, orderId);
        return doCalculateOrderEta(tenantId, order, fingerprint, generation, actor);
    }

    @Override
    public BatchEtaEstimate getBatchEta(UUID batchId) {
        UUID tenantId = requireCurrentTenantId();
        DeliveryBatch batch = batchRepository.findById(tenantId, batchId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_ETA_SUBJECT_NOT_FOUND", "Delivery batch not found: " + batchId));

        List<DeliveryBatchOrder> activeMembers = batchRepository.findActiveOrderMembershipsByBatchId(tenantId, batchId);
        String fingerprint = buildBatchFingerprint(tenantId, batch, activeMembers);

        Optional<BatchEtaEstimate> cached = cachePort.getBatchEta(tenantId, batchId, fingerprint);
        if (cached.isPresent() && !cached.get().isStale(OffsetDateTime.now(ZoneOffset.UTC))) {
            return cached.get();
        }

        long generation = cachePort.beginBatchCalculation(tenantId, batchId);
        return doCalculateBatchEta(tenantId, batch, activeMembers, fingerprint, generation, "system");
    }

    @Override
    public BatchEtaEstimate calculateBatchEta(UUID batchId, String actor) {
        UUID tenantId = requireCurrentTenantId();
        DeliveryBatch batch = batchRepository.findById(tenantId, batchId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_ETA_SUBJECT_NOT_FOUND", "Delivery batch not found: " + batchId));

        List<DeliveryBatchOrder> activeMembers = batchRepository.findActiveOrderMembershipsByBatchId(tenantId, batchId);
        String fingerprint = buildBatchFingerprint(tenantId, batch, activeMembers);
        long generation = cachePort.beginBatchCalculation(tenantId, batchId);
        return doCalculateBatchEta(tenantId, batch, activeMembers, fingerprint, generation, actor);
    }

    private SingleOrderEtaEstimate doCalculateOrderEta(
            UUID tenantId,
            DeliveryOrder order,
            String fingerprint,
            long generation,
            String actor
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        DeliveryLocationLookupPort.LocationReference originLoc = locationLookupPort.findLocation(order.originLocationId())
                .orElseThrow(() -> new BusinessRuleException("DELIVERY_ETA_COORDINATES_MISSING", "Origin location not found"));
        DeliveryLocationLookupPort.LocationReference destLoc = locationLookupPort.findLocation(order.destinationLocationId())
                .orElseThrow(() -> new BusinessRuleException("DELIVERY_ETA_COORDINATES_MISSING", "Destination location not found"));

        if (originLoc.latitude() == null || originLoc.longitude() == null
                || destLoc.latitude() == null || destLoc.longitude() == null) {
            throw new BusinessRuleException("DELIVERY_ETA_COORDINATES_MISSING", "Coordinates are missing for origin or destination");
        }

        LastMileRoutingPort.Coordinate origin = new LastMileRoutingPort.Coordinate(originLoc.latitude(), originLoc.longitude());
        LastMileRoutingPort.Coordinate dest = new LastMileRoutingPort.Coordinate(destLoc.latitude(), destLoc.longitude());

        DeliveryTransportMode mode = requireMode(riderEtaContextPort.findForOrder(tenantId, order.id().value()));
        DeliveryZoneType zoneType = DeliveryZoneType.URBAN_DENSE;

        LastMileRoutingPort.RouteEstimate estimate = routingPort.estimate(origin, dest, mode, zoneType, now);

        OffsetDateTime estimatedArrivalAt = now.plusSeconds(estimate.durationSeconds());
        OffsetDateTime staleAt = now.plusMinutes(FRESHNESS_TTL_MINUTES);

        EtaStatus slaStatus = evaluateOrderSlaStatus(order, estimatedArrivalAt);

        SingleOrderEtaEstimate result = new SingleOrderEtaEstimate(
                order.id().value(),
                estimatedArrivalAt,
                estimate.durationSeconds(),
                estimate.distanceMeters(),
                slaStatus,
                estimate.source(),
                now,
                staleAt
        );

        cachePort.putOrderEtaIfCurrent(tenantId, order.id().value(), generation, fingerprint, result);

        eventPublisherPort.publish(DeliveryEtaCalculatedEvent.of(
                tenantId,
                "ORDER",
                order.id().value(),
                estimatedArrivalAt,
                estimate.durationSeconds(),
                estimate.distanceMeters(),
                slaStatus,
                now,
                actor
        ));

        return result;
    }

    private BatchEtaEstimate doCalculateBatchEta(
            UUID tenantId,
            DeliveryBatch batch,
            List<DeliveryBatchOrder> activeMembers,
            String fingerprint,
            long generation,
            String actor
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (activeMembers == null || activeMembers.isEmpty()) {
            OffsetDateTime staleAt = now.plusMinutes(FRESHNESS_TTL_MINUTES);
            BatchEtaEstimate emptyResult = new BatchEtaEstimate(
                    batch.id(),
                    now,
                    staleAt,
                    0L,
                    0L,
                    now,
                    EtaSource.HEURISTIC,
                    List.of()
            );
            cachePort.putBatchEtaIfCurrent(tenantId, batch.id(), generation, fingerprint, emptyResult);
            return emptyResult;
        }

        DeliveryZone zone = zoneRepository.findById(batch.deliveryZoneId(), tenantId)
                .orElseThrow(() -> new BusinessRuleException("DELIVERY_ETA_INVALID_STATE", "Delivery zone not found for batch"));

        DeliveryZoneType zoneType = zone.zoneType() != null ? zone.zoneType() : DeliveryZoneType.URBAN_DENSE;
        if (batch.riderId() == null) {
            throw new BusinessRuleException("DELIVERY_ETA_INVALID_STATE", "A Rider must be assigned before ETA calculation");
        }
        DeliveryTransportMode mode = requireMode(riderEtaContextPort.findForRider(tenantId, batch.riderId()));

        LastMileRoutingPort.Coordinate currentOrigin = null;
        if (zone.depotLocationId() != null) {
            Optional<DeliveryLocationLookupPort.LocationReference> depotLocOpt = locationLookupPort.findLocation(zone.depotLocationId());
            if (depotLocOpt.isPresent() && depotLocOpt.get().latitude() != null && depotLocOpt.get().longitude() != null) {
                currentOrigin = new LastMileRoutingPort.Coordinate(depotLocOpt.get().latitude(), depotLocOpt.get().longitude());
            }
        }

        List<DeliveryBatchOrder> sortedMembers = new ArrayList<>(activeMembers);
        sortedMembers.sort(Comparator.comparingInt(m -> m.sequenceHint() != null ? m.sequenceHint() : Integer.MAX_VALUE));

        List<BatchEtaStopEstimate> stopEstimates = new ArrayList<>();
        long cumulativeDurationSeconds = 0L;
        long totalDistanceMeters = 0L;
        EtaSource overallSource = EtaSource.HEURISTIC;

        for (int i = 0; i < sortedMembers.size(); i++) {
            DeliveryBatchOrder member = sortedMembers.get(i);
            DeliveryOrder order = orderRepository.findById(member.deliveryOrderId())
                    .orElseThrow(() -> new NotFoundException("DELIVERY_ETA_SUBJECT_NOT_FOUND", "Batch member delivery order not found: " + member.deliveryOrderId()));

            DeliveryLocationLookupPort.LocationReference destLoc = locationLookupPort.findLocation(order.destinationLocationId())
                    .orElseThrow(() -> new BusinessRuleException("DELIVERY_ETA_COORDINATES_MISSING", "Destination location not found for order: " + order.id().value()));

            if (destLoc.latitude() == null || destLoc.longitude() == null) {
                throw new BusinessRuleException("DELIVERY_ETA_COORDINATES_MISSING", "Coordinates missing for order: " + order.id().value());
            }

            LastMileRoutingPort.Coordinate destCoord = new LastMileRoutingPort.Coordinate(destLoc.latitude(), destLoc.longitude());

            if (currentOrigin == null) {
                DeliveryLocationLookupPort.LocationReference originLoc = locationLookupPort.findLocation(order.originLocationId())
                        .orElse(destLoc);
                if (originLoc.latitude() != null && originLoc.longitude() != null) {
                    currentOrigin = new LastMileRoutingPort.Coordinate(originLoc.latitude(), originLoc.longitude());
                } else {
                    currentOrigin = destCoord;
                }
            }

            OffsetDateTime legDepartureTime = now.plusSeconds(cumulativeDurationSeconds);
            LastMileRoutingPort.RouteEstimate legEstimate = routingPort.estimate(currentOrigin, destCoord, mode, zoneType, legDepartureTime);

            if (legEstimate.source() == EtaSource.HEURISTIC_FALLBACK) {
                overallSource = EtaSource.HEURISTIC_FALLBACK;
            }

            long travelDuration = legEstimate.durationSeconds();
            long serviceDuration = resolveServiceBufferSeconds(zoneType);

            cumulativeDurationSeconds += travelDuration;
            OffsetDateTime stopArrivalAt = now.plusSeconds(cumulativeDurationSeconds);
            cumulativeDurationSeconds += serviceDuration;

            totalDistanceMeters += legEstimate.distanceMeters();

            EtaStatus stopSlaStatus = evaluateOrderSlaStatus(order, stopArrivalAt);

            int seq = member.sequenceHint() != null ? member.sequenceHint() : (i + 1);

            stopEstimates.add(new BatchEtaStopEstimate(
                    order.id().value(),
                    seq,
                    stopArrivalAt,
                    travelDuration,
                    serviceDuration,
                    legEstimate.distanceMeters(),
                    stopSlaStatus
            ));

            currentOrigin = destCoord;
        }

        OffsetDateTime estimatedCompletionAt = now.plusSeconds(cumulativeDurationSeconds);
        OffsetDateTime staleAt = now.plusMinutes(FRESHNESS_TTL_MINUTES);

        BatchEtaEstimate batchEstimate = new BatchEtaEstimate(
                batch.id(),
                now,
                staleAt,
                cumulativeDurationSeconds,
                totalDistanceMeters,
                estimatedCompletionAt,
                overallSource,
                stopEstimates
        );

        cachePort.putBatchEtaIfCurrent(tenantId, batch.id(), generation, fingerprint, batchEstimate);

        eventPublisherPort.publish(DeliveryEtaCalculatedEvent.of(
                tenantId,
                "BATCH",
                batch.id(),
                estimatedCompletionAt,
                cumulativeDurationSeconds,
                totalDistanceMeters,
                null,
                now,
                actor
        ));

        return batchEstimate;
    }

    private EtaStatus evaluateOrderSlaStatus(DeliveryOrder order, OffsetDateTime estimatedArrivalAt) {
        if (order.window() == null || order.window().end() == null) {
            return null;
        }

        OffsetDateTime slotEnd = order.window().end();
        OffsetDateTime atRiskThreshold = slotEnd.minusMinutes(15);

        if (!estimatedArrivalAt.isAfter(atRiskThreshold)) {
            return EtaStatus.ON_TIME;
        } else if (!estimatedArrivalAt.isAfter(slotEnd)) {
            return EtaStatus.AT_RISK;
        } else {
            return EtaStatus.LATE;
        }
    }

    private DeliveryTransportMode requireMode(Optional<RiderEtaContextPort.RiderEtaContext> context) {
        RiderEtaContextPort.RiderEtaContext rider = context.orElseThrow(() ->
                new BusinessRuleException("DELIVERY_ETA_INVALID_STATE", "A Rider must be assigned before ETA calculation"));
        if (rider.transportMode() == null) {
            throw new BusinessRuleException("DELIVERY_RIDER_TRANSPORT_MODE_REQUIRED", "Assigned Rider transport mode is not configured");
        }
        return rider.transportMode();
    }

    private long resolveServiceBufferSeconds(DeliveryZoneType zoneType) {
        if (zoneType == DeliveryZoneType.URBAN_DENSE) {
            return APARTMENT_SERVICE_BUFFER_SECONDS; // 10m
        }
        return DOORSTEP_SERVICE_BUFFER_SECONDS; // 5m
    }

    private String buildOrderFingerprint(UUID tenantId, DeliveryOrder order) {
        String raw = tenantId + ":" + order.id().value() + ":" + order.destinationLocationId() + ":"
                + order.status() + ":" + order.version() + ":"
                + riderEtaContextPort.findForOrder(tenantId, order.id().value()).map(RiderEtaContextPort.RiderEtaContext::transportMode).orElse(null);
        return sha256(raw);
    }

    private String buildBatchFingerprint(UUID tenantId, DeliveryBatch batch, List<DeliveryBatchOrder> members) {
        StringBuilder sb = new StringBuilder();
        sb.append(tenantId).append(":").append(batch.id()).append(":").append(batch.version())
                .append(":").append(batch.riderId()).append(":").append(batch.status()).append(":")
                .append(batch.riderId() == null ? null : riderEtaContextPort.findForRider(tenantId, batch.riderId())
                        .map(RiderEtaContextPort.RiderEtaContext::transportMode).orElse(null));

        if (members != null) {
            List<DeliveryBatchOrder> sorted = new ArrayList<>(members);
            sorted.sort(Comparator.comparing(DeliveryBatchOrder::deliveryOrderId));
            for (DeliveryBatchOrder m : sorted) {
                sb.append("|").append(m.deliveryOrderId()).append(":").append(m.sequenceHint()).append(":").append(m.status());
            }
        }
        return sha256(sb.toString());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
