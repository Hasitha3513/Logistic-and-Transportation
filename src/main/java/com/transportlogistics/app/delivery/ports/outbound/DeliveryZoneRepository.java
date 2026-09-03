package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryZoneRepository {
    DeliveryZone save(DeliveryZone zone);
    Optional<DeliveryZone> findById(UUID id, UUID tenantId);
    Optional<DeliveryZone> findByIdForUpdate(UUID id, UUID tenantId);
    Optional<DeliveryZone> findByCode(String zoneCode, UUID tenantId);
    List<DeliveryZone> findActiveCandidatesByBBox(double longitude, double latitude, UUID tenantId);
    List<DeliveryZone> findAll(UUID tenantId, DeliveryZoneStatus status, Boolean serviceable);
    boolean existsByCode(String zoneCode, UUID tenantId);
}
