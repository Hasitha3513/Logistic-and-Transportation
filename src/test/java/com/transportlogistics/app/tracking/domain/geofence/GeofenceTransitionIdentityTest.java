package com.transportlogistics.app.tracking.domain.geofence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GeofenceTransitionIdentityTest {
    @Test
    void identityIsDeterministicAndSensitiveToFrozenFacts() {
        UUID tenant = UUID.randomUUID();
        UUID geofence = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID position = UUID.randomUUID();
        UUID first = identity(tenant, geofence, 3, vehicle, position);

        assertThat(identity(tenant, geofence, 3, vehicle, position)).isEqualTo(first);
        assertThat(identity(UUID.randomUUID(), geofence, 3, vehicle, position)).isNotEqualTo(first);
        assertThat(identity(tenant, geofence, 4, vehicle, position)).isNotEqualTo(first);
        assertThat(identity(tenant, geofence, 3, vehicle, UUID.randomUUID())).isNotEqualTo(first);
        assertThat(first.version()).isEqualTo(5);
    }

    private static UUID identity(UUID tenant, UUID geofence, long definitionVersion,
                                 UUID vehicle, UUID position) {
        return GeofenceTransitionIdentity.create(tenant, geofence, definitionVersion, vehicle,
                GeofenceMembership.OUTSIDE, GeofenceMembership.INSIDE, position);
    }
}
