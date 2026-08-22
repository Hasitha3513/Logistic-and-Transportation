package com.transportlogistics.app.notification.domain.model;

public enum NotificationRuleExecutionOutcome {
    ACCEPTED,
    SUPPRESSED,
    NO_RECIPIENT,
    TEMPLATE_DATA_MISSING,
    FAILED
}
