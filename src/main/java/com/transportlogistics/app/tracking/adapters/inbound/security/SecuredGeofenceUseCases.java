package com.transportlogistics.app.tracking.adapters.inbound.security;

import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceLifecycle;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransition;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.domain.geofence.VehicleGeofenceState;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceManagementUseCase;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceQuery;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

public class SecuredGeofenceUseCases implements GeofenceManagementUseCase, GeofenceQuery {
    private final GeofenceManagementUseCase management;
    private final GeofenceQuery query;

    public SecuredGeofenceUseCases(GeofenceManagementUseCase management, GeofenceQuery query) {
        this.management = management;
        this.query = query;
    }

    @Override
    @PreAuthorize("hasAuthority('GEOFENCE_MANAGE')")
    public Geofence create(Context context, CreateGeofence command, String idempotencyKey) {
        return management.create(context, command, idempotencyKey);
    }

    @Override
    @PreAuthorize("hasAuthority('GEOFENCE_MANAGE')")
    public Geofence update(Context context, UUID geofenceId, long expectedVersion,
                           UpdateGeofence command) {
        return management.update(context, geofenceId, expectedVersion, command);
    }

    @Override
    @PreAuthorize("hasAuthority('GEOFENCE_MANAGE')")
    public Geofence activate(Context context, UUID geofenceId, long expectedVersion,
                             String idempotencyKey) {
        return management.activate(context, geofenceId, expectedVersion, idempotencyKey);
    }

    @Override
    @PreAuthorize("hasAuthority('GEOFENCE_MANAGE')")
    public Geofence disable(Context context, UUID geofenceId, long expectedVersion, String reason,
                            String idempotencyKey) {
        return management.disable(context, geofenceId, expectedVersion, reason, idempotencyKey);
    }

    @Override
    @PreAuthorize("hasAuthority('GEOFENCE_MANAGE')")
    public Geofence retire(Context context, UUID geofenceId, long expectedVersion, String reason,
                           String idempotencyKey) {
        return management.retire(context, geofenceId, expectedVersion, reason, idempotencyKey);
    }

    @Override
    @PreAuthorize("hasAuthority('GEOFENCE_VIEW')")
    public Optional<Geofence> get(UUID tenantId, UUID geofenceId) {
        return query.get(tenantId, geofenceId);
    }

    @Override
    @PreAuthorize("hasAuthority('GEOFENCE_VIEW')")
    public Page<Geofence> list(UUID tenantId, GeofenceType type, GeofenceLifecycle lifecycle,
                               UUID locationId, int page, int size) {
        return query.list(tenantId, type, lifecycle, locationId, page, size);
    }

    @Override
    @PreAuthorize("hasAuthority('GEOFENCE_VIEW')")
    public Page<VehicleGeofenceState> memberships(
            UUID tenantId, UUID vehicleId, UUID geofenceId, int page, int size) {
        return query.memberships(tenantId, vehicleId, geofenceId, page, size);
    }

    @Override
    @PreAuthorize("hasAuthority('GEOFENCE_EVENT_VIEW')")
    public CursorPage<GeofenceTransition> transitions(
            UUID tenantId, UUID geofenceId, UUID vehicleId, GeofenceType type,
            Instant from, Instant to, String cursor, int limit) {
        return query.transitions(tenantId, geofenceId, vehicleId, type, from, to, cursor, limit);
    }

    @Override
    @PreAuthorize("hasAuthority('GEOFENCE_EVENT_VIEW')")
    public CursorPage<GeofenceTransition> unauthorizedTransitions(
            UUID tenantId, Instant from, Instant to, String cursor, int limit) {
        return query.unauthorizedTransitions(tenantId, from, to, cursor, limit);
    }
}
