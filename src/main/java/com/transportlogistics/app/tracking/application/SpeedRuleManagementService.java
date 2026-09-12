package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tracking.domain.speed.SpeedMonitoringException;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import com.transportlogistics.app.tracking.domain.speed.VehicleSpeedState;
import com.transportlogistics.app.tracking.ports.inbound.SpeedMonitoringQuery;
import com.transportlogistics.app.tracking.ports.inbound.SpeedRuleManagementUseCase;
import com.transportlogistics.app.tracking.ports.outbound.SpeedManagementSupportPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedManagementTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedRuleRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleSpeedStateRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SpeedRuleManagementService implements SpeedRuleManagementUseCase, SpeedMonitoringQuery {
    private static final Duration MAX_EPISODE_RANGE = Duration.ofDays(31);
    private final SpeedRuleRepositoryPort rules;
    private final VehicleSpeedStateRepositoryPort states;
    private final SpeedingEpisodeRepositoryPort episodes;
    private final SpeedManagementSupportPort support;
    private final SpeedManagementTransactionPort transaction;

    public SpeedRuleManagementService(SpeedRuleRepositoryPort rules,
                                      VehicleSpeedStateRepositoryPort states,
                                      SpeedingEpisodeRepositoryPort episodes,
                                      SpeedManagementSupportPort support,
                                      SpeedManagementTransactionPort transaction) {
        this.rules = rules;
        this.states = states;
        this.episodes = episodes;
        this.support = support;
        this.transaction = transaction;
    }

    @Override
    public SpeedRule create(Context context, CreateRule command, String idempotencyKey) {
        Context current = required(context);
        String key = requiredKey(idempotencyKey);
        String requestHash = hash(command.name() + "|" + command.scope() + "|" + command.routeId()
                + "|" + command.routeVersion() + "|" + command.thresholdKph());
        UUID targetId = UUID.nameUUIDFromBytes((current.tenantId() + "|SPEED_RULE_CREATE|" + key)
                .getBytes(StandardCharsets.UTF_8));
        return transaction.execute(() -> {
            var claim = support.claim(current.tenantId(), "CREATE", key, requestHash, targetId,
                    current.actorId(), current.now());
            requireSameRequest(claim.requestHash(), requestHash);
            if (!claim.acquired()) return requiredRule(current.tenantId(), claim.targetId());
            SpeedRule rule = construct(targetId, current.tenantId(), command.name(), command.scope(),
                    command.routeId(), command.routeVersion(), command.thresholdKph());
            SpeedRule saved = save(rule, 0);
            support.audit(current.tenantId(), current.actorId(), "SPEED_RULE_CREATED", saved.id(),
                    detail(null, saved.lifecycle(), null, saved.ruleVersion(), saved), current.now());
            support.complete(claim.claimId(), saved.ruleVersion());
            return saved;
        });
    }

    @Override
    public SpeedRule update(Context context, UUID ruleId, long expectedVersion, UpdateRule command) {
        Context current = required(context);
        return transaction.execute(() -> {
            SpeedRule rule = requiredRule(current.tenantId(), ruleId);
            requireVersion(rule, expectedVersion);
            long before = rule.ruleVersion();
            SpeedRule changed;
            try {
                changed = rule.update(command.name(), command.thresholdKph(), command.routeId(),
                        command.routeVersion());
            } catch (SpeedMonitoringException exception) {
                throw translated(exception);
            }
            SpeedRule saved = save(changed, expectedVersion);
            support.audit(current.tenantId(), current.actorId(), "SPEED_RULE_UPDATED", saved.id(),
                    detail(rule.lifecycle(), saved.lifecycle(), before, saved.ruleVersion(), saved),
                    current.now());
            return saved;
        });
    }

    @Override public SpeedRule activate(Context context, UUID id, long version, String key) {
        return lifecycle(context, id, version, null, key, "ACTIVATE",
                (rule, now) -> rule.activate(now));
    }
    @Override public SpeedRule disable(Context context, UUID id, long version, String reason, String key) {
        return lifecycle(context, id, version, requiredReason(reason), key, "DISABLE",
                (rule, now) -> rule.disable());
    }
    @Override public SpeedRule retire(Context context, UUID id, long version, String reason, String key) {
        return lifecycle(context, id, version, requiredReason(reason), key, "RETIRE",
                (rule, now) -> rule.retire());
    }

    @Override public Optional<SpeedRule> rule(UUID tenantId, UUID ruleId) {
        return rules.findRule(required(tenantId, "Tenant ID"), required(ruleId, "Rule ID"));
    }
    @Override public Page<SpeedRule> rules(UUID tenantId, SpeedRule.Scope scope,
                                           SpeedRule.Lifecycle lifecycle, int page, int size) {
        requirePage(page, size);
        UUID tenant = required(tenantId, "Tenant ID");
        return new Page<>(rules.find(tenant, scope, lifecycle, page, size), page, size,
                rules.count(tenant, scope, lifecycle));
    }
    @Override public Optional<VehicleSpeedState> state(UUID tenantId, UUID vehicleId) {
        return states.findState(required(tenantId, "Tenant ID"), required(vehicleId, "Vehicle ID"));
    }
    @Override public Page<VehicleSpeedState> states(UUID tenantId,
                                                    VehicleSpeedState.MonitoringState state,
                                                    int page, int size) {
        requirePage(page, size);
        UUID tenant = required(tenantId, "Tenant ID");
        return new Page<>(states.find(tenant, state, page, size), page, size,
                states.count(tenant, state));
    }
    @Override public Optional<SpeedingEpisode> episode(UUID tenantId, UUID episodeId) {
        return episodes.findEpisode(required(tenantId, "Tenant ID"), required(episodeId, "Episode ID"));
    }
    @Override public CursorPage<SpeedingEpisode> episodes(UUID tenantId, UUID vehicleId,
                                                          UUID driverId, Instant from, Instant to,
                                                          String cursor, int limit) {
        required(from, "From source time");
        required(to, "To source time");
        if (from.isAfter(to) || Duration.between(from, to).compareTo(MAX_EPISODE_RANGE) > 0) {
            throw new BusinessRuleException("SPEED_EPISODE_RANGE_INVALID",
                    "Episode source-time range must be ordered and no greater than 31 days");
        }
        if (limit < 1 || limit > 500) {
            throw new BusinessRuleException("SPEED_EPISODE_PAGE_INVALID",
                    "Episode limit must be between 1 and 500");
        }
        List<SpeedingEpisode> found = episodes.find(required(tenantId, "Tenant ID"), vehicleId,
                driverId, from, to, cursor, limit + 1);
        boolean more = found.size() > limit;
        List<SpeedingEpisode> items = more ? found.subList(0, limit) : found;
        return new CursorPage<>(items, more ? cursor(items.getLast()) : null);
    }

    private SpeedRule lifecycle(Context context, UUID id, long version, String reason, String keyValue,
                                String action, LifecycleMutation mutation) {
        Context current = required(context);
        String key = requiredKey(keyValue);
        String requestHash = hash(id + "|" + version + "|" + action + "|" + Objects.toString(reason, ""));
        return transaction.execute(() -> {
            var claim = support.claim(current.tenantId(), action, key, requestHash, id,
                    current.actorId(), current.now());
            requireSameRequest(claim.requestHash(), requestHash);
            if (!claim.acquired()) return requiredRule(current.tenantId(), claim.targetId());
            SpeedRule rule = requiredRule(current.tenantId(), id);
            requireVersion(rule, version);
            SpeedRule changed;
            try {
                changed = mutation.apply(rule, current.now());
            } catch (SpeedMonitoringException exception) {
                throw translated(exception);
            }
            SpeedRule saved = save(changed, version);
            support.audit(current.tenantId(), current.actorId(), "SPEED_RULE_" + action + "D",
                    saved.id(), detail(rule.lifecycle(), saved.lifecycle(), rule.ruleVersion(),
                            saved.ruleVersion(), saved) + ";REASON=" + Objects.toString(reason, "NONE"),
                    current.now());
            support.complete(claim.claimId(), saved.ruleVersion());
            return saved;
        });
    }

    private static SpeedRule construct(UUID id, UUID tenant, String name, SpeedRule.Scope scope,
                                       UUID routeId, String routeVersion,
                                       com.transportlogistics.app.tracking.domain.speed.SpeedKph threshold) {
        try {
            return new SpeedRule(id, tenant, name, scope, routeId, routeVersion, threshold,
                    SpeedRule.Lifecycle.DRAFT, 1, null);
        } catch (SpeedMonitoringException exception) {
            throw translated(exception);
        }
    }
    private SpeedRule requiredRule(UUID tenantId, UUID id) {
        return rules.findRule(tenantId, required(id, "Rule ID")).orElseThrow(() ->
                new NotFoundException("SPEED_RULE_NOT_FOUND", "Speed rule not found"));
    }
    private SpeedRule save(SpeedRule rule, long expectedVersion) {
        try {
            return rules.save(rule, expectedVersion);
        } catch (BusinessRuleException exception) {
            if (exception.code().equals("SPEED_RULE_STALE_VERSION")
                    || exception.code().equals("SPEED_RULE_CONFLICT")) {
                throw new ConflictException(exception.code(), exception.getMessage(), exception);
            }
            throw exception;
        }
    }
    private static void requireVersion(SpeedRule rule, long expected) {
        if (expected < 1 || rule.ruleVersion() != expected) {
            throw new ConflictException("SPEED_RULE_STALE_VERSION", "Speed rule version is stale");
        }
    }
    private static void requireSameRequest(String stored, String requested) {
        if (!stored.equals(requested)) throw new ConflictException("IDEMPOTENCY_KEY_CONFLICT",
                "Idempotency-Key was already used for another request");
    }
    private static void requirePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BusinessRuleException(
                "SPEED_MONITOR_PAGE_INVALID", "Page must be non-negative and size between 1 and 100");
    }
    private static String requiredKey(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 160) throw new BusinessRuleException(
                "IDEMPOTENCY_KEY_INVALID", "Idempotency-Key must contain 1 to 160 characters");
        return value.trim();
    }
    private static String requiredReason(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 300) throw new BusinessRuleException(
                "SPEED_RULE_REASON_INVALID", "Reason must contain 1 to 300 characters");
        return value.trim();
    }
    private static Context required(Context context) {
        Objects.requireNonNull(context, "Speed-rule context is required");
        required(context.tenantId(), "Tenant ID"); required(context.actorId(), "Actor ID");
        required(context.now(), "Current time"); return context;
    }
    private static <T> T required(T value, String name) {
        return Objects.requireNonNull(value, name + " is required");
    }
    private static String detail(SpeedRule.Lifecycle before, SpeedRule.Lifecycle after,
                                 Long beforeVersion, long afterVersion, SpeedRule rule) {
        return "LIFECYCLE=" + Objects.toString(before, "NONE") + "->" + after
                + ";VERSION=" + Objects.toString(beforeVersion, "NONE") + "->" + afterVersion
                + ";SCOPE=" + rule.scope() + ";THRESHOLD_KPH=" + rule.thresholdKph().value()
                + ";ROUTE_ID=" + Objects.toString(rule.routeId(), "NONE")
                + ";ROUTE_VERSION=" + Objects.toString(rule.routeVersion(), "NONE");
    }
    private static String cursor(SpeedingEpisode episode) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                (episode.startSourceTimestamp() + "|" + episode.id()).getBytes(StandardCharsets.UTF_8));
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private static BusinessRuleException translated(SpeedMonitoringException exception) {
        return new BusinessRuleException(exception.code(), exception.getMessage());
    }
    @FunctionalInterface private interface LifecycleMutation {
        SpeedRule apply(SpeedRule rule, Instant now);
    }
}
