package com.transportlogistics.app.notification.application.ports.out;

import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public interface SmsNotificationSenderPort {
    SendResult send(SendRequest request) throws InterruptedException;

    record SendRequest(UUID notificationId, String idempotencyKey, String to,
                       String message, Duration timeout) {
        public SendRequest {
            Objects.requireNonNull(notificationId);
            Objects.requireNonNull(idempotencyKey);
            Objects.requireNonNull(to);
            Objects.requireNonNull(message);
            Objects.requireNonNull(timeout);
        }
    }

    record SendResult(boolean accepted, String providerMessageId, EmailDeliveryErrorCategory errorCategory,
                      String errorCode, String errorMessage) {
        public SendResult {
            if (accepted && errorCategory != null) {
                throw new IllegalArgumentException("Accepted result cannot have an error");
            }
            if (!accepted && errorCategory == null) {
                throw new IllegalArgumentException("Rejected result requires an error category");
            }
        }

        public static SendResult accepted(String providerMessageId) {
            return new SendResult(true, providerMessageId, null, null, null);
        }

        public static SendResult rejected(EmailDeliveryErrorCategory category, String code, String message) {
            return new SendResult(false, null, category, code, message);
        }
    }
}
