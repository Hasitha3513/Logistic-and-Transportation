package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryZoneRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DeliveryZonePersistenceAdapter implements DeliveryZoneRepository {

    private final DeliveryZoneJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public DeliveryZonePersistenceAdapter(DeliveryZoneJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public DeliveryZone save(DeliveryZone zone) {
        DeliveryZoneEntity entity = jpaRepository.findByIdAndTenantId(zone.id(), zone.tenantId())
                .orElseGet(DeliveryZoneEntity::new);

        entity.setId(zone.id());
        entity.setTenantId(zone.tenantId());
        entity.setZoneCode(zone.zoneCode());
        entity.setZoneName(zone.zoneName());
        entity.setDescription(zone.description());
        entity.setZoneType(zone.zoneType());
        entity.setStatus(zone.status());
        entity.setServiceable(zone.serviceable());
        entity.setDailyCapacity(zone.dailyCapacity());
        entity.setDepotLocationId(zone.depotLocationId());
        entity.setMinLatitude(zone.boundary().boundingBox().minLatitude());
        entity.setMaxLatitude(zone.boundary().boundingBox().maxLatitude());
        entity.setMinLongitude(zone.boundary().boundingBox().minLongitude());
        entity.setMaxLongitude(zone.boundary().boundingBox().maxLongitude());
        entity.setBoundaryGeoJson(toGeoJson(zone.boundary()));
        entity.setPriority(zone.priority());
        entity.setCreatedAt(zone.createdAt());
        entity.setCreatedBy(zone.createdBy());
        entity.setUpdatedAt(zone.updatedAt());
        entity.setUpdatedBy(zone.updatedBy());
        if (zone.version() != null) {
            entity.setVersion(zone.version());
        }

        DeliveryZoneEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryZone> findById(UUID id, UUID tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public Optional<DeliveryZone> findByIdForUpdate(UUID id, UUID tenantId) {
        return jpaRepository.findByIdAndTenantIdWithLock(id, tenantId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryZone> findByCode(String zoneCode, UUID tenantId) {
        return jpaRepository.findByZoneCodeAndTenantId(zoneCode.trim().toUpperCase(), tenantId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryZone> findActiveCandidatesByBBox(double longitude, double latitude, UUID tenantId) {
        return jpaRepository.findActiveCandidatesByBBox(tenantId, latitude, longitude)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryZone> findAll(UUID tenantId, DeliveryZoneStatus status, Boolean serviceable) {
        return jpaRepository.findAllByTenant(tenantId, status, serviceable)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String zoneCode, UUID tenantId) {
        return jpaRepository.existsByZoneCodeAndTenantId(zoneCode.trim().toUpperCase(), tenantId);
    }

    private String toGeoJson(DeliveryZoneBoundary boundary) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "Polygon");
            ArrayNode coordinatesNode = root.putArray("coordinates");
            ArrayNode ringNode = coordinatesNode.addArray();
            for (DeliveryZoneCoordinate c : boundary.coordinates()) {
                ArrayNode coord = ringNode.addArray();
                coord.add(c.longitude());
                coord.add(c.latitude());
            }
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to serialize GeoJSON boundary", e);
        }
    }

    private DeliveryZone toDomain(DeliveryZoneEntity entity) {
        DeliveryZoneBoundary boundary = parseGeoJson(entity.getBoundaryGeoJson());
        return new DeliveryZone(
                entity.getId(),
                entity.getTenantId(),
                entity.getZoneCode(),
                entity.getZoneName(),
                entity.getDescription(),
                entity.getZoneType(),
                entity.getStatus(),
                entity.isServiceable(),
                entity.getDailyCapacity(),
                entity.getDepotLocationId(),
                boundary,
                entity.getPriority(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    private DeliveryZoneBoundary parseGeoJson(String geoJson) {
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            String type = root.path("type").asText();
            if (!"Polygon".equalsIgnoreCase(type)) {
                throw new IllegalArgumentException("Unsupported GeoJSON type: " + type + ". Expected Polygon.");
            }
            JsonNode coordinatesNode = root.path("coordinates");
            if (!coordinatesNode.isArray() || coordinatesNode.isEmpty()) {
                throw new IllegalArgumentException("Invalid GeoJSON Polygon coordinates array");
            }
            JsonNode ringNode = coordinatesNode.get(0);
            if (!ringNode.isArray() || ringNode.isEmpty()) {
                throw new IllegalArgumentException("Invalid GeoJSON linear ring");
            }

            List<DeliveryZoneCoordinate> coords = new ArrayList<>();
            for (JsonNode point : ringNode) {
                if (!point.isArray() || point.size() < 2) {
                    throw new IllegalArgumentException("Coordinate pair must contain [longitude, latitude]");
                }
                double lon = point.get(0).asDouble();
                double lat = point.get(1).asDouble();
                coords.add(new DeliveryZoneCoordinate(lon, lat));
            }
            return new DeliveryZoneBoundary(coords);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to parse GeoJSON boundary", e);
        }
    }
}
