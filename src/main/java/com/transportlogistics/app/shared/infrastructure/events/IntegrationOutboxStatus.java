package com.transportlogistics.app.shared.infrastructure.events;

enum IntegrationOutboxStatus {
    PENDING,
    PROCESSING,
    RETRY,
    PUBLISHED,
    FAILED,
    UNSUPPORTED
}
