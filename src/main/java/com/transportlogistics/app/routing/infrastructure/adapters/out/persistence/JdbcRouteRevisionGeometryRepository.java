package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.PlannedRouteGeometry;
import com.transportlogistics.app.routing.Wgs84Point;
import com.transportlogistics.app.routing.application.ports.out.RouteRevisionGeometryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
final class JdbcRouteRevisionGeometryRepository implements RouteRevisionGeometryRepository {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    JdbcRouteRevisionGeometryRepository(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override
    public PlannedRouteGeometry save(
            UUID tenantId, UUID routeRevisionId, PlannedRouteGeometry geometry) {
        if (tenantId == null || routeRevisionId == null || geometry == null) {
            throw new IllegalArgumentException("Tenant, revision and geometry are required");
        }
        PlannedRouteGeometry existing = find(tenantId, geometry.routeId(), geometry.routeVersion())
                .orElse(null);
        if (existing != null) {
            if (existing.equals(geometry)) return existing;
            throw new IllegalStateException("Published route revision geometry is immutable");
        }
        UUID geometryId = UUID.randomUUID();
        try {
            transactions.executeWithoutResult(status -> {
                Integer revision = jdbc.queryForObject("""
                        SELECT revision_number FROM route_revision
                        WHERE id=? AND tenant_id=? AND route_id=?
                        """, Integer.class, routeRevisionId, tenantId, geometry.routeId());
                if (revision == null || !geometry.routeVersion().equals("REVISION:" + revision)) {
                    throw new IllegalArgumentException("Geometry must match the exact Routing revision");
                }
                jdbc.update("""
                        INSERT INTO route_revision_geometry(
                          id,tenant_id,route_revision_id,route_id,route_version,point_count)
                        VALUES(?,?,?,?,?,?)
                        """, geometryId, tenantId, routeRevisionId, geometry.routeId(),
                        geometry.routeVersion(), geometry.orderedPoints().size());
                for (int index = 0; index < geometry.orderedPoints().size(); index++) {
                    Wgs84Point point = geometry.orderedPoints().get(index);
                    jdbc.update("""
                            INSERT INTO route_revision_geometry_point(
                              tenant_id,geometry_id,point_order,longitude,latitude)
                            VALUES(?,?,?,?,?)
                            """, tenantId, geometryId, index, point.longitude(), point.latitude());
                }
            });
            return geometry;
        } catch (DuplicateKeyException exception) {
            return find(tenantId, geometry.routeId(), geometry.routeVersion())
                    .filter(geometry::equals)
                    .orElseThrow(() -> new IllegalStateException(
                            "Published route revision geometry is immutable", exception));
        }
    }

    @Override
    public Optional<PlannedRouteGeometry> find(UUID tenantId, UUID routeId, String routeVersion) {
        if (tenantId == null || routeId == null || routeVersion == null
                || routeVersion.length() > 120
                || !routeVersion.matches("REVISION:[1-9][0-9]*")) {
            throw new IllegalArgumentException("Exact Tenant, route and canonical revision are required");
        }
        var ids = jdbc.query("""
                SELECT id FROM route_revision_geometry
                WHERE tenant_id=? AND route_id=? AND route_version=?
                """, (row, number) -> UUID.fromString(row.getString(1)), tenantId, routeId, routeVersion);
        if (ids.isEmpty()) return Optional.empty();
        var points = jdbc.query("""
                SELECT longitude,latitude FROM route_revision_geometry_point
                WHERE tenant_id=? AND geometry_id=? ORDER BY point_order
                """, (row, number) -> new Wgs84Point(
                        row.getBigDecimal(1), row.getBigDecimal(2)), tenantId, ids.getFirst());
        return Optional.of(new PlannedRouteGeometry(routeId, routeVersion, points));
    }
}
