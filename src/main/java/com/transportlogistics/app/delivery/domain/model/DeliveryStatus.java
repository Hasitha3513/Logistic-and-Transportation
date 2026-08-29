package com.transportlogistics.app.delivery.domain.model;

/** Architectural lifecycle baseline from the frozen contract; not a completed workflow implementation. */
public enum DeliveryStatus {
    DRAFT,
    READY_FOR_ASSIGNMENT,
    ASSIGNED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    REDELIVERY_SCHEDULED,
    RETURN_TO_ORIGIN,
    CANCELLED
}
