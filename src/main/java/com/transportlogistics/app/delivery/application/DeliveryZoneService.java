package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneUseCase;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryZoneRepository;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DeliveryZoneService implements DeliveryZoneUseCase, DeliveryZoneLookupPort {

    private final DeliveryZoneRepository zoneRepository;
    private final DeliveryLocationLookupPort locationLookupPort;
    private final DeliveryTenantContextPort tenantContext;
    private final DeliveryOrderTransaction transactions;
    private final Clock clock;

    public DeliveryZoneService(
            DeliveryZoneRepository zoneRepository,
            DeliveryLocationLookupPort locationLookupPort,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            Clock clock
    ) {
        this.zoneRepository = zoneRepository;
        this.locationLookupPort = locationLookupPort;
        this.tenantContext = tenantContext;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public DeliveryZone createZone(CreateZoneCommand command, String actor) {
        return transactions.execute(() -> {
            UUID tenantId = requireTenantId();
            OffsetDateTime now = now();

            if (zoneRepository.existsByCode(command.zoneCode(), tenantId)) {
                throw new ConflictException("DELIVERY_ZONE_CODE_DUPLICATE", "Zone code already exists: " + command.zoneCode());
            }

            if (command.depotLocationId() != null) {
                validateDepotLocation(command.depotLocationId());
            }

            DeliveryZoneBoundary boundary = new DeliveryZoneBoundary(command.coordinates());

            DeliveryZone zone = DeliveryZone.create(
                    tenantId,
                    command.zoneCode(),
                    command.zoneName(),
                    command.description(),
                    command.zoneType(),
                    command.serviceable(),
                    command.dailyCapacity(),
                    command.depotLocationId(),
                    boundary,
                    command.priority(),
                    actor != null ? actor : "system",
                    now
            );

            return zoneRepository.save(zone);
        });
    }

    @Override
    public DeliveryZone updateZone(UUID id, UpdateZoneCommand command, String actor) {
        return transactions.execute(() -> {
            UUID tenantId = requireTenantId();
            OffsetDateTime now = now();

            DeliveryZone zone = zoneRepository.findById(id, tenantId)
                    .orElseThrow(() -> new NotFoundException("DeliveryZone", "Delivery zone not found: " + id));

            if (!Objects.equals(zone.version(), command.expectedVersion())) {
                throw new ConflictException("DELIVERY_ZONE_VERSION_CONFLICT", "Expected version " + command.expectedVersion() + " but found " + zone.version());
            }

            if (command.depotLocationId() != null) {
                validateDepotLocation(command.depotLocationId());
            }

            DeliveryZoneBoundary boundary = new DeliveryZoneBoundary(command.coordinates());

            zone.update(
                    command.zoneName(),
                    command.description(),
                    command.zoneType(),
                    command.serviceable(),
                    command.dailyCapacity(),
                    command.depotLocationId(),
                    boundary,
                    command.priority(),
                    actor != null ? actor : "system",
                    now
            );

            return zoneRepository.save(zone);
        });
    }

    @Override
    public DeliveryZone activateZone(UUID id, String actor) {
        return transactions.execute(() -> {
            UUID tenantId = requireTenantId();
            OffsetDateTime now = now();

            DeliveryZone zone = zoneRepository.findById(id, tenantId)
                    .orElseThrow(() -> new NotFoundException("DeliveryZone", "Delivery zone not found: " + id));

            zone.activate(actor != null ? actor : "system", now);
            return zoneRepository.save(zone);
        });
    }

    @Override
    public DeliveryZone deactivateZone(UUID id, String actor) {
        return transactions.execute(() -> {
            UUID tenantId = requireTenantId();
            OffsetDateTime now = now();

            DeliveryZone zone = zoneRepository.findById(id, tenantId)
                    .orElseThrow(() -> new NotFoundException("DeliveryZone", "Delivery zone not found: " + id));

            zone.deactivate(actor != null ? actor : "system", now);
            return zoneRepository.save(zone);
        });
    }

    @Override
    public DeliveryZone getZone(UUID id) {
        UUID tenantId = requireTenantId();
        return zoneRepository.findById(id, tenantId)
                .orElseThrow(() -> new NotFoundException("DeliveryZone", "Delivery zone not found: " + id));
    }

    @Override
    public Optional<DeliveryZone> findZone(UUID id) {
        UUID tenantId = requireTenantId();
        return zoneRepository.findById(id, tenantId);
    }

    @Override
    public Optional<DeliveryZone> findZoneForUpdate(UUID id) {
        UUID tenantId = requireTenantId();
        return zoneRepository.findByIdForUpdate(id, tenantId);
    }

    @Override
    public List<DeliveryZone> listZones(DeliveryZoneStatus status, Boolean serviceable) {
        UUID tenantId = requireTenantId();
        return zoneRepository.findAll(tenantId, status, serviceable);
    }

    @Override
    public Optional<DeliveryZone> resolveZoneForCoordinates(double longitude, double latitude) {
        UUID tenantId = requireTenantId();
        List<DeliveryZone> candidates = zoneRepository.findActiveCandidatesByBBox(longitude, latitude, tenantId);

        return candidates.stream()
                .filter(z -> z.contains(longitude, latitude))
                .sorted(
                        Comparator.comparingInt(DeliveryZone::priority).reversed()
                                .thenComparingDouble(z -> z.boundary().approximateArea())
                                .thenComparing(DeliveryZone::updatedAt, Comparator.reverseOrder())
                                .thenComparing(DeliveryZone::id)
                )
                .findFirst();
    }

    @Override
    public Optional<DeliveryZone> resolveZoneForLocation(UUID locationId) {
        if (locationId == null) {
            return Optional.empty();
        }
        var loc = locationLookupPort.findLocation(locationId)
                .orElseThrow(() -> new NotFoundException("Location", "Location not found: " + locationId));

        if (loc.longitude() == null || loc.latitude() == null) {
            return Optional.empty();
        }

        return resolveZoneForCoordinates(loc.longitude(), loc.latitude());
    }

    @Override
    public boolean isLocationServiceable(UUID locationId) {
        return resolveZoneForLocation(locationId)
                .map(DeliveryZone::serviceable)
                .orElse(false);
    }

    @Override
    public List<DeliveryZone> listActiveZones() {
        return listZones(DeliveryZoneStatus.ACTIVE, null);
    }

    private void validateDepotLocation(UUID depotLocationId) {
        var loc = locationLookupPort.findLocation(depotLocationId)
                .orElseThrow(() -> new NotFoundException("DepotLocation", "Depot location not found: " + depotLocationId));
        if (!loc.active()) {
            throw new BusinessRuleException("DEPOT_LOCATION_INACTIVE", "Depot location is inactive: " + depotLocationId);
        }
    }

    private UUID requireTenantId() {
        return tenantContext.currentTenant()
                .map(DeliveryTenantContextPort.TenantContext::tenantId)
                .orElseThrow(() -> new BusinessRuleException("TENANT_REQUIRED", "Tenant context is required for delivery zone operations"));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
