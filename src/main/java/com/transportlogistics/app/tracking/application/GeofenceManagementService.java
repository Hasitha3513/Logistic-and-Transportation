package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceLifecycle;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceRuleException;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransition;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.domain.geofence.VehicleGeofenceState;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceManagementUseCase;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceQuery;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceLocationLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceManagementSupportPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceManagementTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleGeofenceStateRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GeofenceManagementService implements GeofenceManagementUseCase, GeofenceQuery {
    private static final int MAX_ACTIVE = 500;
    private final GeofenceRepositoryPort geofences;
    private final VehicleGeofenceStateRepositoryPort states;
    private final GeofenceTransitionRepositoryPort transitions;
    private final GeofenceLocationLookupPort locations;
    private final GeofenceManagementSupportPort support;
    private final GeofenceManagementTransactionPort transaction;

    public GeofenceManagementService(
            GeofenceRepositoryPort geofences, VehicleGeofenceStateRepositoryPort states,
            GeofenceTransitionRepositoryPort transitions, GeofenceLocationLookupPort locations,
            GeofenceManagementSupportPort support, GeofenceManagementTransactionPort transaction) {
        this.geofences = geofences;
        this.states = states;
        this.transitions = transitions;
        this.locations = locations;
        this.support = support;
        this.transaction = transaction;
    }

    @Override
    public Geofence create(Context context, CreateGeofence command, String idempotencyKey) {
        Context current = required(context);
        String key = requiredKey(idempotencyKey);
        String requestHash = hash(createMaterial(command));
        UUID targetId = UUID.nameUUIDFromBytes((current.tenantId() + "|GEOFENCE_CREATE|" + key)
                .getBytes(StandardCharsets.UTF_8));
        return transaction.execute(() -> {
            var claim = support.claim(current.tenantId(), "CREATE", key, requestHash, targetId,
                    current.actorId(), current.now());
            if (!claim.requestHash().equals(requestHash)) {
                throw conflict();
            }
            if (!claim.acquired()) {
                return getRequired(current.tenantId(), claim.targetId());
            }
            validateLocation(current.tenantId(), command.type(), command.locationId());
            Geofence created;
            try {
                created = Geofence.draft(targetId, current.tenantId(), command.name(),
                        command.type(), command.polygon(), command.locationId(), command.alertPolicy(),
                        current.now(), current.actorId());
            } catch (GeofenceRuleException exception) {
                throw translated(exception);
            }
            Geofence saved = geofences.save(created, 0);
            support.audit(current.tenantId(), current.actorId(), "GEOFENCE_CREATED", saved.id(),
                    detail(null, saved.lifecycle(), null, saved.version(), "definition"), current.now());
            support.complete(claim.claimId(), saved.version());
            return saved;
        });
    }

    @Override
    public Geofence update(Context context, UUID geofenceId, long expectedVersion,
                           UpdateGeofence command) {
        Context current = required(context);
        return transaction.execute(() -> {
            Geofence geofence = locked(current.tenantId(), geofenceId);
            requireVersion(geofence, expectedVersion);
            validateLocation(current.tenantId(), command.type(), command.locationId());
            long before = geofence.version();
            GeofenceLifecycle lifecycle = geofence.lifecycle();
            try {
                geofence.updateDefinition(command.name(), command.type(), command.polygon(),
                        command.locationId(), command.alertPolicy(), current.now(), current.actorId());
            } catch (GeofenceRuleException exception) {
                throw translated(exception);
            }
            Geofence saved = geofences.save(geofence, expectedVersion);
            support.audit(current.tenantId(), current.actorId(), "GEOFENCE_UPDATED", saved.id(),
                    detail(lifecycle, saved.lifecycle(), before, saved.version(),
                            "name,type,polygon,locationId,alertPolicy"), current.now());
            return saved;
        });
    }

    @Override
    public Geofence activate(Context context, UUID geofenceId, long expectedVersion,
                             String idempotencyKey) {
        return lifecycle(context, geofenceId, expectedVersion, null, idempotencyKey,
                "ACTIVATE", Geofence::activate);
    }

    @Override
    public Geofence disable(Context context, UUID geofenceId, long expectedVersion, String reason,
                            String idempotencyKey) {
        return lifecycle(context, geofenceId, expectedVersion, requiredReason(reason), idempotencyKey,
                "DISABLE", Geofence::disable);
    }

    @Override
    public Geofence retire(Context context, UUID geofenceId, long expectedVersion, String reason,
                           String idempotencyKey) {
        return lifecycle(context, geofenceId, expectedVersion, requiredReason(reason), idempotencyKey,
                "RETIRE", Geofence::retire);
    }

    @Override
    public java.util.Optional<Geofence> get(UUID tenantId, UUID geofenceId) {
        return geofences.find(required(tenantId, "Tenant ID"), required(geofenceId, "Geofence ID"));
    }

    @Override
    public Page<Geofence> list(UUID tenantId, GeofenceType type, GeofenceLifecycle lifecycle,
                               UUID locationId, int page, int size) {
        requirePage(page, size);
        UUID tenant = required(tenantId, "Tenant ID");
        return new Page<>(geofences.find(tenant, type, lifecycle, locationId, page, size), page, size,
                geofences.count(tenant, type, lifecycle, locationId));
    }

    @Override
    public Page<VehicleGeofenceState> memberships(UUID tenantId, UUID vehicleId,
                                                   UUID geofenceId, int page, int size) {
        requirePage(page, size);
        UUID tenant = required(tenantId, "Tenant ID");
        return new Page<>(states.find(tenant, vehicleId, geofenceId, page, size), page, size,
                states.count(tenant, vehicleId, geofenceId));
    }

    @Override
    public CursorPage<GeofenceTransition> transitions(
            UUID tenantId, UUID geofenceId, UUID vehicleId, GeofenceType type,
            Instant from, Instant to, String cursor, int limit) {
        return transitionPage(tenantId, geofenceId, vehicleId, type, from, to, cursor, limit, false);
    }

    @Override
    public CursorPage<GeofenceTransition> unauthorizedTransitions(
            UUID tenantId, Instant from, Instant to, String cursor, int limit) {
        return transitionPage(tenantId, null, null, null, from, to, cursor, limit, true);
    }

    private Geofence lifecycle(
            Context context, UUID id, long version, String reason, String idempotencyKey,
            String action, LifecycleMutation mutation) {
        Context current = required(context);
        String key = requiredKey(idempotencyKey);
        String requestHash = hash(id + "|" + version + "|" + action + "|" + Objects.toString(reason, ""));
        return transaction.execute(() -> {
            var claim = support.claim(current.tenantId(), action, key, requestHash, id,
                    current.actorId(), current.now());
            if (!claim.requestHash().equals(requestHash)) {
                throw conflict();
            }
            if (!claim.acquired()) {
                return getRequired(current.tenantId(), claim.targetId());
            }
            Geofence geofence = locked(current.tenantId(), id);
            requireVersion(geofence, version);
            GeofenceLifecycle beforeLifecycle = geofence.lifecycle();
            long beforeVersion = geofence.version();
            if ("ACTIVATE".equals(action)) {
                validateLocation(current.tenantId(), geofence.type(), geofence.locationId());
                if (geofences.countActiveForUpdate(current.tenantId()) >= MAX_ACTIVE) {
                    throw new BusinessRuleException(
                            "GEOFENCE_ACTIVE_LIMIT", "Tenant cannot exceed 500 active geofences");
                }
            }
            try {
                mutation.apply(geofence, current.now(), current.actorId());
            } catch (GeofenceRuleException exception) {
                throw translated(exception);
            }
            Geofence saved = geofences.save(geofence, version);
            support.audit(current.tenantId(), current.actorId(), "GEOFENCE_" + action + "D",
                    saved.id(), detail(beforeLifecycle, saved.lifecycle(), beforeVersion,
                            saved.version(), reason), current.now());
            support.complete(claim.claimId(), saved.version());
            return saved;
        });
    }

    private CursorPage<GeofenceTransition> transitionPage(
            UUID tenantId, UUID geofenceId, UUID vehicleId, GeofenceType type,
            Instant from, Instant to, String cursor, int limit, boolean unauthorized) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Source-time range is invalid");
        }
        List<GeofenceTransition> found = transitions.find(required(tenantId, "Tenant ID"),
                geofenceId, vehicleId, type, from, to, cursor, limit + 1, unauthorized);
        boolean more = found.size() > limit;
        List<GeofenceTransition> items = more ? found.subList(0, limit) : found;
        String next = more ? cursor(items.getLast()) : null;
        return new CursorPage<>(items, next);
    }

    private void validateLocation(UUID tenantId, GeofenceType type, UUID locationId) {
        if (type == GeofenceType.DEPOT || type == GeofenceType.CUSTOMER_SITE) {
            if (locationId == null) {
                throw new BusinessRuleException(
                        "GEOFENCE_LOCATION_REQUIRED", "Location-backed geofences require a location");
            }
            if (locations.findActive(tenantId, locationId).filter(value -> value.active()).isEmpty()) {
                throw new BusinessRuleException(
                        "GEOFENCE_LOCATION_NOT_FOUND", "Active same-Tenant location not found");
            }
        }
    }

    private Geofence locked(UUID tenantId, UUID id) {
        return geofences.findForUpdate(tenantId, required(id, "Geofence ID"))
                .orElseThrow(GeofenceManagementService::notFound);
    }

    private Geofence getRequired(UUID tenantId, UUID id) {
        return geofences.find(tenantId, id).orElseThrow(GeofenceManagementService::notFound);
    }

    private static void requireVersion(Geofence geofence, long expected) {
        if (expected < 0 || geofence.version() != expected) {
            throw new BusinessRuleException(
                    "GEOFENCE_STALE_VERSION", "Geofence version is stale or unavailable");
        }
    }

    private static void requirePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size between 1 and 100");
        }
    }

    private static String createMaterial(CreateGeofence command) {
        return command.name().trim() + "|" + command.type() + "|" + command.locationId() + "|"
                + command.alertPolicy() + "|" + command.polygon().vertices();
    }

    private static String detail(GeofenceLifecycle before, GeofenceLifecycle after,
                                 Long beforeVersion, long afterVersion, String fact) {
        return "LIFECYCLE=" + Objects.toString(before, "NONE") + "->" + after
                + ";VERSION=" + Objects.toString(beforeVersion, "NONE") + "->" + afterVersion
                + ";FACT=" + Objects.toString(fact, "NONE");
    }

    private static String requiredKey(String value) {
        if (value == null || value.isBlank() || value.length() > 160) {
            throw new BusinessRuleException(
                    "IDEMPOTENCY_KEY_INVALID", "Idempotency-Key must contain 1 to 160 characters");
        }
        return value.trim();
    }

    private static String requiredReason(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 300) {
            throw new BusinessRuleException(
                    "GEOFENCE_REASON_REQUIRED", "Reason must contain 1 to 300 characters");
        }
        return value.trim();
    }

    private static Context required(Context context) {
        Objects.requireNonNull(context, "Geofence context is required");
        required(context.tenantId(), "Tenant ID");
        required(context.actorId(), "Actor ID");
        Objects.requireNonNull(context.now(), "Current time is required");
        return context;
    }

    private static <T> T required(T value, String name) {
        return Objects.requireNonNull(value, name + " is required");
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String cursor(GeofenceTransition transition) {
        return transition.sourceTimestamp() + "|" + transition.transitionId();
    }

    private static BusinessRuleException conflict() {
        return new BusinessRuleException(
                "IDEMPOTENCY_KEY_CONFLICT", "Idempotency-Key was already used for another request");
    }

    private static BusinessRuleException translated(GeofenceRuleException exception) {
        return new BusinessRuleException(exception.code(), exception.getMessage());
    }

    private static NotFoundException notFound() {
        return new NotFoundException("GEOFENCE_NOT_FOUND", "Geofence not found");
    }

    @FunctionalInterface
    private interface LifecycleMutation {
        void apply(Geofence geofence, Instant now, UUID actorId);
    }
}
