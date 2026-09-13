package com.transportlogistics.app.routing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteAssignmentLookupContractTest {
    @Test
    void publishesCanonicalCurrentRevisionWithoutInternalTypes() {
        var route = new RouteAssignmentLookup.AssignmentRoute(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), true, "REVISION:27");
        assertEquals("REVISION:27", route.routeVersion());
    }

    @Test
    void rejectsNonCanonicalOrUnboundedVersions() {
        assertDoesNotThrow(() -> route("REVISION:1"));
        assertThrows(IllegalArgumentException.class, () -> route("REVISION:0"));
        assertThrows(IllegalArgumentException.class, () -> route("REVISION:-2"));
        assertThrows(IllegalArgumentException.class, () -> route(" REVISION:1"));
        assertThrows(IllegalArgumentException.class, () -> route("R".repeat(121)));
    }

    private RouteAssignmentLookup.AssignmentRoute route(String version) {
        return new RouteAssignmentLookup.AssignmentRoute(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                true, version);
    }
}
