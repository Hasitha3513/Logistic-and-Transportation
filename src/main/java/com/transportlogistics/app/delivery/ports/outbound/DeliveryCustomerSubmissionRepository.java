package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.CustomerSubmissionType;
import com.transportlogistics.app.delivery.domain.model.DeliveryCustomerSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryCustomerSubmissionRepository {
    DeliveryCustomerSubmission save(DeliveryCustomerSubmission submission);
    Optional<DeliveryCustomerSubmission> findIdempotent(UUID accessId, CustomerSubmissionType type, String key);
    boolean feedbackExists(UUID deliveryId, UUID customerId);
    long countRecent(UUID deliveryId, java.time.OffsetDateTime after);
    List<DeliveryCustomerSubmission> findByDelivery(UUID deliveryId, UUID customerId);
}
