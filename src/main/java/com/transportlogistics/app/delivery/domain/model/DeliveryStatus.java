package com.transportlogistics.app.delivery.domain.model;

public enum DeliveryStatus {
    DRAFT,
    READY_FOR_ASSIGNMENT,
    DELIVERED,
    FAILED_ATTEMPT,
    RETURN_TO_BASE,
    ESCALATED
}
