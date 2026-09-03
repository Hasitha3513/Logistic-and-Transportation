package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

public enum DeliveryFailureReason {
    CUSTOMER_UNAVAILABLE(DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, true, 0),
    WRONG_ADDRESS(DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, true, 0),
    CUSTOMER_REFUSED(DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED, false, 5),
    ACCESS_RESTRICTED(DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, true, 0),
    DAMAGED_CARGO(DeliveryFailureDisposition.ESCALATED, false, 5),
    DOCUMENT_OR_PAYMENT_ISSUE(DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, true, 0),
    OTHER(null, true, 10);

    private final DeliveryFailureDisposition defaultDisposition;
    private final boolean redeliveryEligible;
    private final int minimumNotesLength;

    DeliveryFailureReason(DeliveryFailureDisposition defaultDisposition, boolean redeliveryEligible, int minimumNotesLength) {
        this.defaultDisposition = defaultDisposition;
        this.redeliveryEligible = redeliveryEligible;
        this.minimumNotesLength = minimumNotesLength;
    }

    public DeliveryFailureDisposition getDefaultDisposition() {
        return defaultDisposition;
    }

    public boolean isRedeliveryEligible() {
        return redeliveryEligible;
    }

    public int getMinimumNotesLength() {
        return minimumNotesLength;
    }

    public void validateNotes(String notes) {
        String trimmed = notes == null ? "" : notes.trim();
        if (minimumNotesLength > 0 && trimmed.length() < minimumNotesLength) {
            throw new BusinessRuleException("INVALID_FAILURE_NOTES",
                    "Notes of at least " + minimumNotesLength + " characters required for reason " + name());
        }
    }

    public DeliveryFailureDisposition resolveDisposition(DeliveryFailureDisposition requestedDisposition) {
        if (this == OTHER) {
            if (requestedDisposition == null) {
                throw new BusinessRuleException("DISPOSITION_REQUIRED", "Explicit disposition is required for reason OTHER");
            }
            return requestedDisposition;
        }
        if (this == DAMAGED_CARGO) {
            if (requestedDisposition != null && (requestedDisposition == DeliveryFailureDisposition.ESCALATED
                    || requestedDisposition == DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED)) {
                return requestedDisposition;
            }
            return defaultDisposition;
        }
        return defaultDisposition;
    }
}
