package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DeliveryZonePersistenceAdapter.class, ObjectMapper.class})
class DeliveryZonePersistencePostgreSqlAcceptanceTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private DeliveryZonePersistenceAdapter adapter;

    @MockBean
    private DeliveryTenantContextPort tenantContext;

    private final OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        when(tenantContext.currentTenant())
                .thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(TENANT_A, "UTC")));
    }

    @Test
    @DisplayName("Saves and retrieves DeliveryZone with GeoJSON boundary and bounding box")
    void saveAndFindZone() {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(79.8, 6.9),
                new DeliveryZoneCoordinate(79.9, 6.9),
                new DeliveryZoneCoordinate(79.9, 7.0),
                new DeliveryZoneCoordinate(79.8, 7.0),
                new DeliveryZoneCoordinate(79.8, 6.9)
        );
        DeliveryZone zone = DeliveryZone.create(
                TENANT_A,
                "ZONE-PG-1",
                "Colombo North",
                "Urban area",
                DeliveryZoneType.URBAN_DENSE,
                true,
                200,
                null,
                new DeliveryZoneBoundary(coords),
                10,
                "admin",
                now
        );

        DeliveryZone saved = adapter.save(zone);
        assertThat(saved.id()).isNotNull();

        Optional<DeliveryZone> fetched = adapter.findById(saved.id(), TENANT_A);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().zoneCode()).isEqualTo("ZONE-PG-1");
        assertThat(fetched.get().boundary().coordinates()).hasSize(5);
        assertThat(fetched.get().boundary().boundingBox().minLatitude()).isEqualTo(6.9);
    }

    @Test
    @DisplayName("Enforces tenant isolation: Tenant B cannot view Tenant A zone")
    void tenantIsolation() {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(10.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 10.0),
                new DeliveryZoneCoordinate(20.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 20.0),
                new DeliveryZoneCoordinate(10.0, 10.0)
        );
        DeliveryZone zoneA = DeliveryZone.create(
                TENANT_A,
                "ZONE-TENANT-A",
                "Zone A",
                null,
                DeliveryZoneType.SUBURBAN,
                true,
                null,
                null,
                new DeliveryZoneBoundary(coords),
                0,
                "admin",
                now
        );
        DeliveryZone savedA = adapter.save(zoneA);

        assertThat(adapter.findById(savedA.id(), TENANT_B)).isEmpty();
    }

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    @Test
    @DisplayName("Enforces unique zone_code per tenant")
    void duplicateCodeWithinTenantRejected() {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(0.0, 0.0),
                new DeliveryZoneCoordinate(1.0, 0.0),
                new DeliveryZoneCoordinate(1.0, 1.0),
                new DeliveryZoneCoordinate(0.0, 1.0),
                new DeliveryZoneCoordinate(0.0, 0.0)
        );
        DeliveryZone zone1 = DeliveryZone.create(
                TENANT_A,
                "ZONE-UNIQUE",
                "Zone 1",
                null,
                DeliveryZoneType.RURAL,
                true,
                null,
                null,
                new DeliveryZoneBoundary(coords),
                0,
                "admin",
                now
        );
        adapter.save(zone1);
        entityManager.flush();

        DeliveryZone zone2 = DeliveryZone.create(
                TENANT_A,
                "ZONE-UNIQUE",
                "Zone 2",
                null,
                DeliveryZoneType.RURAL,
                true,
                null,
                null,
                new DeliveryZoneBoundary(coords),
                0,
                "admin",
                now
        );

        assertThatThrownBy(() -> {
            adapter.save(zone2);
            entityManager.flush();
        }).satisfies(throwable -> {
            assertThat(throwable).isInstanceOfAny(
                    DataIntegrityViolationException.class,
                    org.hibernate.exception.ConstraintViolationException.class,
                    jakarta.persistence.PersistenceException.class
            );
        });
    }
}
