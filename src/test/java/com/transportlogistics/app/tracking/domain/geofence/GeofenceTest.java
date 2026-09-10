package com.transportlogistics.app.tracking.domain.geofence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GeofenceTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID LOCATION = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-10T00:00:00Z");

    @Test
    void trimsNameAndEnforcesLocationRules() {
        Geofence depot = draft(GeofenceType.DEPOT, LOCATION, policy(), "  Depot  ");
        assertThat(depot.name()).isEqualTo("Depot");

        assertThatThrownBy(() -> draft(GeofenceType.DEPOT, null, policy(), "Depot"))
                .hasFieldOrPropertyWithValue("code", "GEOFENCE_LOCATION_REQUIRED");
        assertThatThrownBy(() -> draft(GeofenceType.CUSTOMER_SITE, null, policy(), "Site"))
                .hasFieldOrPropertyWithValue("code", "GEOFENCE_LOCATION_REQUIRED");
        assertThatThrownBy(() -> draft(GeofenceType.UNAUTHORIZED_ZONE, LOCATION,
                GeofenceAlertPolicy.unauthorizedZone(), "Restricted"))
                .hasFieldOrPropertyWithValue("code", "GEOFENCE_LOCATION_INVALID");
    }

    @Test
    void unauthorizedEntryAlertCannotBeDisabled() {
        assertThatThrownBy(() -> draft(GeofenceType.UNAUTHORIZED_ZONE, null,
                new GeofenceAlertPolicy(false, true), "Restricted"))
                .hasFieldOrPropertyWithValue("code", "GEOFENCE_POLICY_INVALID");
    }

    @Test
    void supportsFrozenLifecycleAndRetiredIsTerminal() {
        Geofence geofence = draft(GeofenceType.DEPOT, LOCATION, policy(), "Depot");
        geofence.activate(NOW.plusSeconds(1), ACTOR);
        assertThat(geofence.lifecycle()).isEqualTo(GeofenceLifecycle.ACTIVE);
        geofence.disable(NOW.plusSeconds(2), ACTOR);
        geofence.activate(NOW.plusSeconds(3), ACTOR);
        geofence.disable(NOW.plusSeconds(4), ACTOR);
        geofence.retire(NOW.plusSeconds(5), ACTOR);
        assertThat(geofence.lifecycle()).isEqualTo(GeofenceLifecycle.RETIRED);
        assertThatThrownBy(() -> geofence.activate(NOW.plusSeconds(6), ACTOR))
                .hasFieldOrPropertyWithValue("code", "GEOFENCE_LIFECYCLE_INVALID");
    }

    @Test
    void draftCanRetireButActiveCannotRetireDirectly() {
        Geofence draft = draft(GeofenceType.DEPOT, LOCATION, policy(), "First");
        draft.retire(NOW.plusSeconds(1), ACTOR);
        assertThat(draft.lifecycle()).isEqualTo(GeofenceLifecycle.RETIRED);

        Geofence active = draft(GeofenceType.DEPOT, LOCATION, policy(), "Second");
        active.activate(NOW.plusSeconds(1), ACTOR);
        assertThatThrownBy(() -> active.retire(NOW.plusSeconds(2), ACTOR))
                .hasFieldOrPropertyWithValue("code", "GEOFENCE_LIFECYCLE_INVALID");
    }

    @Test
    void definitionMutationIsAllowedOnlyInDraftOrDisabled() {
        Geofence geofence = draft(GeofenceType.DEPOT, LOCATION, policy(), "Depot");
        geofence.updateDefinition("Site", GeofenceType.CUSTOMER_SITE, polygon(), LOCATION,
                policy(), NOW.plusSeconds(1), ACTOR);
        geofence.activate(NOW.plusSeconds(2), ACTOR);
        assertThatThrownBy(() -> geofence.updateDefinition("Changed", GeofenceType.DEPOT,
                polygon(), LOCATION, policy(), NOW.plusSeconds(3), ACTOR))
                .hasFieldOrPropertyWithValue("code", "GEOFENCE_LIFECYCLE_INVALID");
        geofence.disable(NOW.plusSeconds(4), ACTOR);
        geofence.updateDefinition("Changed", GeofenceType.DEPOT, polygon(), LOCATION,
                policy(), NOW.plusSeconds(5), ACTOR);
        assertThat(geofence.name()).isEqualTo("Changed");
    }

    private static Geofence draft(GeofenceType type, UUID locationId,
                                   GeofenceAlertPolicy alertPolicy, String name) {
        return Geofence.draft(UUID.randomUUID(), TENANT, name, type, polygon(), locationId,
                alertPolicy, NOW, ACTOR);
    }

    private static GeofencePolygon polygon() {
        return GeofencePolygon.of(List.of(new Wgs84Coordinate(0, 0),
                new Wgs84Coordinate(10, 0), new Wgs84Coordinate(0, 10)));
    }

    private static GeofenceAlertPolicy policy() {
        return new GeofenceAlertPolicy(true, true);
    }
}
