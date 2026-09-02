package com.transportlogistics.app.delivery;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Public Delivery fact contract consumed by Notification. */
public record DeliveryCustomerNotificationEvent(
    UUID eventId,
    String eventType,
    UUID tenantId,
    OffsetDateTime occurredAt,
    int version,
    String aggregateType,
    UUID aggregateId,
    Map<String, String> payload
) {
    private static final Set<String> EVENT_TYPES = Set.of(
        "DELIVERY_OUT_FOR_DELIVERY",
        "DELIVERY_ETA_RISK_CHANGED",
        "DELIVERY_COMPLETED",
        "DELIVERY_FAILED_ATTEMPT_RECORDED",
        "DELIVERY_REDELIVERY_SCHEDULED"
    );

    public DeliveryCustomerNotificationEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        if (!EVENT_TYPES.contains(eventType)) throw new IllegalArgumentException("Unsupported Delivery event type");
        if (version != 1) throw new IllegalArgumentException("Delivery notification event version must be 1");
        if (!"DELIVERY_ORDER".equals(aggregateType)) {
            throw new IllegalArgumentException("Delivery notification aggregate type must be DELIVERY_ORDER");
        }
        payload = Map.copyOf(Objects.requireNonNull(payload, "payload must not be null"));
        Set<String> allowedFields = switch (eventType) {
            case "DELIVERY_OUT_FOR_DELIVERY" ->
                Set.of("customerId", "deliveryNumber", "actor", "status");
            case "DELIVERY_ETA_RISK_CHANGED" ->
                Set.of("customerId", "deliveryNumber", "actor", "estimatedArrivalAt", "slaStatus");
            case "DELIVERY_COMPLETED" ->
                Set.of("customerId", "deliveryNumber", "actor", "status", "completedAt");
            case "DELIVERY_FAILED_ATTEMPT_RECORDED" ->
                Set.of("customerId", "deliveryNumber", "actor", "status", "failureDisposition");
            case "DELIVERY_REDELIVERY_SCHEDULED" -> Set.of("customerId", "deliveryNumber", "actor", "status",
                "scheduleId", "scheduledWindowStart", "scheduledWindowEnd");
            default -> throw new IllegalArgumentException("Unsupported Delivery event type");
        };
        if (!payload.keySet().equals(allowedFields)
                || payload.values().stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("Delivery notification payload does not match the frozen contract");
        }
    }

    public static DeliveryCustomerNotificationEvent create(String eventType, UUID tenantId, UUID deliveryOrderId,
                                                            OffsetDateTime occurredAt, Map<String, String> payload) {
        return new DeliveryCustomerNotificationEvent(UUID.randomUUID(), eventType, tenantId, occurredAt, 1,
            "DELIVERY_ORDER", deliveryOrderId, payload);
    }
}
