package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneUseCase;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryZoneRepository;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryZoneServiceTest {

    @Mock
    private DeliveryZoneRepository zoneRepository;

    @Mock
    private DeliveryLocationLookupPort locationLookupPort;

    @Mock
    private DeliveryTenantContextPort tenantContext;

    private DeliveryZoneService service;
    private final UUID tenantId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();
    private final DeliveryOrderTransaction transactions = new DeliveryOrderTransaction() {
        @Override
        public <T> T execute(java.util.function.Supplier<T> operation) {
            return operation.get();
        }
    };
    private final Clock clock = Clock.fixed(now.toInstant(), java.time.ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        when(tenantContext.currentTenant())
                .thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(tenantId, "UTC")));
        service = new DeliveryZoneService(zoneRepository, locationLookupPort, tenantContext, transactions, clock);
    }

    @Test
    @DisplayName("Creates zone successfully when zone code is unique")
    void createZoneSuccess() {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(10.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 10.0)
        );
        DeliveryZoneUseCase.CreateZoneCommand command = new DeliveryZoneUseCase.CreateZoneCommand(
                "ZONE-101",
                "Zone 101",
                "Description",
                DeliveryZoneType.URBAN_DENSE,
                true,
                150,
                null,
                coords,
                5
        );

        when(zoneRepository.existsByCode("ZONE-101", tenantId)).thenReturn(false);
        when(zoneRepository.save(any(DeliveryZone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryZone created = service.createZone(command, "admin");

        assertThat(created.zoneCode()).isEqualTo("ZONE-101");
        assertThat(created.priority()).isEqualTo(5);
    }

    @Test
    @DisplayName("Rejects creation when duplicate zone code exists in tenant")
    void rejectDuplicateCode() {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(10.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 10.0)
        );
        DeliveryZoneUseCase.CreateZoneCommand command = new DeliveryZoneUseCase.CreateZoneCommand(
                "ZONE-DUP",
                "Zone Dup",
                "Desc",
                DeliveryZoneType.SUBURBAN,
                true,
                null,
                null,
                coords,
                0
        );

        when(zoneRepository.existsByCode("ZONE-DUP", tenantId)).thenReturn(true);

        assertThatThrownBy(() -> service.createZone(command, "admin"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).code()).isEqualTo("DELIVERY_ZONE_CODE_DUPLICATE"));
    }

    @Test
    @DisplayName("Resolves zone using priority and area for overlapping candidates")
    void resolveOverlappingZoneWithPriority() {
        // Broad zone with lower priority
        DeliveryZone broadZone = createZoneInstance("ZONE-BROAD", 1, List.of(
                new DeliveryZoneCoordinate(0.0, 0.0),
                new DeliveryZoneCoordinate(30.0, 0.0),
                new DeliveryZoneCoordinate(30.0, 30.0),
                new DeliveryZoneCoordinate(0.0, 30.0),
                new DeliveryZoneCoordinate(0.0, 0.0)
        ));

        // Narrow micro-zone with higher priority
        DeliveryZone microZone = createZoneInstance("ZONE-MICRO", 10, List.of(
                new DeliveryZoneCoordinate(10.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 10.0)
        ));

        when(zoneRepository.findActiveCandidatesByBBox(15.0, 15.0, tenantId))
                .thenReturn(List.of(broadZone, microZone));

        Optional<DeliveryZone> resolved = service.resolveZoneForCoordinates(15.0, 15.0);

        assertThat(resolved).isPresent();
        assertThat(resolved.get().zoneCode()).isEqualTo("ZONE-MICRO");
    }

    @Test
    @DisplayName("Resolves zone for location via LocationLookupPort")
    void resolveZoneForLocation() {
        UUID locId = UUID.randomUUID();
        when(locationLookupPort.findLocation(locId)).thenReturn(Optional.of(
                new DeliveryLocationLookupPort.LocationReference(locId, "LOC-1", "Depot 1", "Address", 15.0, 15.0, true)
        ));

        DeliveryZone zone = createZoneInstance("ZONE-LOC", 5, List.of(
                new DeliveryZoneCoordinate(10.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 10.0)
        ));
        when(zoneRepository.findActiveCandidatesByBBox(15.0, 15.0, tenantId)).thenReturn(List.of(zone));

        Optional<DeliveryZone> resolved = service.resolveZoneForLocation(locId);

        assertThat(resolved).isPresent();
        assertThat(resolved.get().zoneCode()).isEqualTo("ZONE-LOC");
        assertThat(service.isLocationServiceable(locId)).isTrue();
    }

    private DeliveryZone createZoneInstance(String code, int priority, List<DeliveryZoneCoordinate> coords) {
        return new DeliveryZone(
                UUID.randomUUID(),
                tenantId,
                code,
                code + " Name",
                "Desc",
                DeliveryZoneType.URBAN_DENSE,
                DeliveryZoneStatus.ACTIVE,
                true,
                100,
                null,
                new DeliveryZoneBoundary(coords),
                priority,
                0L,
                now,
                "admin",
                now,
                "admin"
        );
    }
}
