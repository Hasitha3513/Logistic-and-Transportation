package com.transportlogistics.app.delivery.domain.model;

public enum DeliveryExceptionResolutionCode {
    RETURN_TO_BASE_APPROVED,
    ACCEPTED_AS_IS,
    REDELIVERY_APPROVED,
    ADDRESS_CORRECTED,
    PARTIAL_ACCEPTED_CLOSE,
    OTP_OVERRIDDEN_BY_MANAGER,
    NEW_OTP_REQUESTED,
    REFUSAL_CONFIRMED_RTO
}
