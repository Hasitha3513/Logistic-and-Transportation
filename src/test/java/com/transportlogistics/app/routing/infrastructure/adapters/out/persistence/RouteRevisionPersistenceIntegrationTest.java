package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteRevisionRepository;
import com.transportlogistics.app.routing.domain.model.Route;
import com.transportlogistics.app.routing.domain.model.RouteRevision;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.locations;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RouteRevisionPersistenceIntegrationTest {
    @Autowired RouteRepository routes;
    @Autowired RouteRevisionRepository revisionRepo;
    @Autowired JdbcTemplate jdbc;

    @Test
    void persistsAndRetrievesOrderedRevisions() {
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var stop1 = UUID.randomUUID();
        var stop2 = UUID.randomUUID();
        locations(jdbc, origin, destination, stop1, stop2);

        var route = new Route(UUID.randomUUID(), "RT-REV-" + suffix(), "Revision Test Route",
                origin, destination, 120.0, 100, true, List.of(stop1, stop2));
        routes.save(route);

        var rev1 = RouteRevision.from(route, 1, OffsetDateTime.now(), "creator");
        revisionRepo.save(rev1);

        var updatedRoute = new Route(route.id(), route.code(), "Updated Route Name",
                origin, destination, 130.0, 110, true, List.of(stop2));
        routes.save(updatedRoute);

        var rev2 = RouteRevision.from(updatedRoute, 2, OffsetDateTime.now(), "modifier");
        revisionRepo.save(rev2);

        var revisions = revisionRepo.findByRouteIdOrderByRevisionNumberDesc(route.id());
        assertEquals(2, revisions.size());
        assertEquals(2, revisions.get(0).revisionNumber());
        assertEquals("Updated Route Name", revisions.get(0).name());
        assertEquals(List.of(stop2), revisions.get(0).stopLocationIds());

        assertEquals(1, revisions.get(1).revisionNumber());
        assertEquals("Revision Test Route", revisions.get(1).name());
        assertEquals(List.of(stop1, stop2), revisions.get(1).stopLocationIds());

        assertEquals(2, revisionRepo.findLatestRevisionNumber(route.id()));
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
