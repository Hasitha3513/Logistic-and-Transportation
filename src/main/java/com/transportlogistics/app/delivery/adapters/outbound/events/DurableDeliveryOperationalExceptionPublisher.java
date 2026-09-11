package com.transportlogistics.app.delivery.adapters.outbound.events;

import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionCase;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOperationalExceptionPublisher;
import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
class DurableDeliveryOperationalExceptionPublisher implements DeliveryOperationalExceptionPublisher {
    private final DurableEventPublisher events;
    private final CurrentTenant currentTenant;

    DurableDeliveryOperationalExceptionPublisher(DurableEventPublisher events, CurrentTenant currentTenant) {
        this.events = events;
        this.currentTenant = currentTenant;
    }

    @Override
    public void publish(DeliveryExceptionCase exceptionCase) {
        var context = currentTenant.required();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("deliveryOrderId", exceptionCase.deliveryOrderId().value().toString());
        if (exceptionCase.deliveryAttemptId() != null) {
            metadata.put("deliveryAttemptId", exceptionCase.deliveryAttemptId().toString());
        }
        events.publish(new OperationalExceptionFactV1(exceptionCase.id(), context.tenantId(),
            OperationalExceptionFactV1.SourceModule.DELIVERY, exceptionCase.exceptionType().name(),
            exceptionCase.id(), exceptionCase.reportedAt(),
            OperationalExceptionFactV1.Severity.valueOf(exceptionCase.severity().name()),
            category(exceptionCase), "DELIVERY_EXCEPTION_CREATED", Map.copyOf(metadata), context.correlationId()));
    }

    private static OperationalExceptionFactV1.Category category(DeliveryExceptionCase value) {
        return switch (value.exceptionType()) {
            case OTP_MISMATCH -> OperationalExceptionFactV1.Category.SECURITY;
            case WRONG_ADDRESS, RECIPIENT_REFUSAL -> OperationalExceptionFactV1.Category.CUSTOMER;
            case DAMAGED_DELIVERY, PARTIAL_DELIVERY -> OperationalExceptionFactV1.Category.OPERATIONAL;
        };
    }
}
