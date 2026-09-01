package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import com.transportlogistics.app.delivery.ports.outbound.RiderEtaContextPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class DeliveryRiderEtaContextAdapter implements RiderEtaContextPort {

    private final DeliveryRiderRepository repository;

    public DeliveryRiderEtaContextAdapter(DeliveryRiderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RiderEtaContext> findForRider(UUID tenantId, UUID riderId) {
        return repository.findById(riderId, tenantId)
                .map(rider -> new RiderEtaContext(rider.getId(), rider.getTransportMode()));
    }

    @Override
    public Optional<RiderEtaContext> findForOrder(UUID tenantId, UUID deliveryOrderId) {
        return repository.findActiveAssignmentForOrder(deliveryOrderId, tenantId)
                .flatMap(assignment -> findForRider(tenantId, assignment.getRiderId()));
    }
}
