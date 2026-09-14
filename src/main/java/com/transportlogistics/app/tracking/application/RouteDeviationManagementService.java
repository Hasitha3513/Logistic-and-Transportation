package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tracking.domain.routedeviation.DistanceMeters;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationException;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationReview;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteVersion;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationQueryUseCase;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationReviewUseCase;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationRuleManagementUseCase;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationManagementSupportPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationManagementTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationReviewRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationRuleRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationStateRepositoryPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RouteDeviationManagementService implements RouteDeviationRuleManagementUseCase,
        RouteDeviationReviewUseCase, RouteDeviationQueryUseCase {
    private static final Duration MAX_RANGE = Duration.ofDays(31);
    private final RouteDeviationRuleRepositoryPort rules;
    private final RouteDeviationStateRepositoryPort states;
    private final RouteDeviationEpisodeRepositoryPort episodes;
    private final RouteDeviationReviewRepositoryPort reviews;
    private final RouteDeviationManagementSupportPort support;
    private final RouteDeviationManagementTransactionPort transactions;

    public RouteDeviationManagementService(RouteDeviationRuleRepositoryPort rules,
            RouteDeviationStateRepositoryPort states, RouteDeviationEpisodeRepositoryPort episodes,
            RouteDeviationReviewRepositoryPort reviews,
            RouteDeviationManagementSupportPort support,
            RouteDeviationManagementTransactionPort transactions) {
        this.rules = rules;
        this.states = states;
        this.episodes = episodes;
        this.reviews = reviews;
        this.support = support;
        this.transactions = transactions;
    }

    @Override
    public RouteDeviationRule create(RouteDeviationRuleManagementUseCase.Context context,
            UUID routeId, String routeVersion, BigDecimal tolerance, String idempotencyKey) {
        var current = required(context);
        String key = key(idempotencyKey);
        String requestHash = hash(routeId + "|" + routeVersion + "|" + tolerance);
        UUID targetId = UUID.nameUUIDFromBytes((current.tenantId() + "|ROUTE_DEVIATION_CREATE|" + key)
                .getBytes(StandardCharsets.UTF_8));
        return transactions.execute(() -> {
            var claim = support.claim(current.tenantId(), "RULE_CREATE", key, requestHash,
                    targetId, current.actorId(), current.now());
            sameRequest(claim, requestHash);
            if (!claim.acquired()) return requiredRule(current.tenantId(), claim.targetId());
            RouteDeviationRule saved;
            try {
                saved = save(new RouteDeviationRule(targetId, current.tenantId(),
                        required(routeId, "Route ID"), new RouteVersion(routeVersion),
                        new DistanceMeters(tolerance), RouteDeviationRule.Lifecycle.DRAFT,
                        0, 0, null));
            } catch (RouteDeviationException exception) {
                throw conflictOrRule(exception);
            }
            support.audit(current.tenantId(), current.actorId(), "ROUTE_DEVIATION_RULE_CREATED",
                    "ROUTE_DEVIATION_RULE", saved.id(), detail(null, saved), current.now());
            support.complete(claim.claimId(), saved.manageVersion());
            return saved;
        });
    }

    @Override
    public RouteDeviationRule update(RouteDeviationRuleManagementUseCase.Context context, UUID id,
            long expectedVersion, BigDecimal tolerance, String idempotencyKey) {
        return ruleCommand(context, id, expectedVersion, "RULE_UPDATE", id + "|" + expectedVersion
                + "|" + tolerance, idempotencyKey, null,
                rule -> rule.update(new DistanceMeters(tolerance)));
    }

    @Override
    public RouteDeviationRule activate(RouteDeviationRuleManagementUseCase.Context context, UUID id,
            long expectedVersion, String idempotencyKey) {
        return ruleCommand(context, id, expectedVersion, "RULE_ACTIVATE",
                id + "|" + expectedVersion, idempotencyKey, null,
                rule -> rule.activate(context.now()));
    }

    @Override
    public RouteDeviationRule disable(RouteDeviationRuleManagementUseCase.Context context, UUID id,
            long expectedVersion, String reason, String idempotencyKey) {
        return ruleCommand(context, id, expectedVersion, "RULE_DISABLE",
                id + "|" + expectedVersion + "|" + reason(reason), idempotencyKey,
                reason(reason), RouteDeviationRule::disable);
    }

    @Override
    public RouteDeviationRule retire(RouteDeviationRuleManagementUseCase.Context context, UUID id,
            long expectedVersion, String reason, String idempotencyKey) {
        return ruleCommand(context, id, expectedVersion, "RULE_RETIRE",
                id + "|" + expectedVersion + "|" + reason(reason), idempotencyKey,
                reason(reason), RouteDeviationRule::retire);
    }

    private RouteDeviationRule ruleCommand(RouteDeviationRuleManagementUseCase.Context context,
            UUID id, long expectedVersion, String action, String request, String idempotencyKey,
            String reason, RuleMutation mutation) {
        var current = required(context);
        String key = key(idempotencyKey);
        String requestHash = hash(request);
        return transactions.execute(() -> {
            var claim = support.claim(current.tenantId(), action, key, requestHash,
                    required(id, "Rule ID"), current.actorId(), current.now());
            sameRequest(claim, requestHash);
            if (!claim.acquired()) return requiredRule(current.tenantId(), claim.targetId());
            RouteDeviationRule before = requiredRule(current.tenantId(), id);
            version(before.manageVersion(), expectedVersion, "ROUTE_DEVIATION_RULE_STALE_VERSION");
            RouteDeviationRule saved;
            try {
                saved = save(mutation.apply(before));
            } catch (RouteDeviationException exception) {
                throw conflictOrRule(exception);
            }
            support.audit(current.tenantId(), current.actorId(),
                    "ROUTE_DEVIATION_" + action, "ROUTE_DEVIATION_RULE", saved.id(),
                    detail(before, saved) + ";REASON=" + Objects.toString(reason, "NONE"),
                    current.now());
            support.complete(claim.claimId(), saved.manageVersion());
            return saved;
        });
    }

    @Override
    public RouteDeviationReview approve(RouteDeviationReviewUseCase.Context context, UUID episodeId,
            long expectedVersion, RouteDeviationReview.Reason reason, String note, String key) {
        return decide(context, episodeId, expectedVersion, RouteDeviationReview.Status.APPROVED,
                reason, note, key, false);
    }

    @Override
    public RouteDeviationReview reject(RouteDeviationReviewUseCase.Context context, UUID episodeId,
            long expectedVersion, RouteDeviationReview.Reason reason, String note, String key) {
        return decide(context, episodeId, expectedVersion, RouteDeviationReview.Status.REJECTED,
                reason, note, key, false);
    }

    @Override
    public RouteDeviationReview correct(RouteDeviationReviewUseCase.Context context, UUID episodeId,
            long expectedVersion, RouteDeviationReview.Status status,
            RouteDeviationReview.Reason reason, String note, String key) {
        if (status != RouteDeviationReview.Status.APPROVED
                && status != RouteDeviationReview.Status.REJECTED) {
            throw rule("ROUTE_DEVIATION_REVIEW_STATUS_INVALID",
                    "Corrected review status must be APPROVED or REJECTED");
        }
        return decide(context, episodeId, expectedVersion, status, reason, note, key, true);
    }

    private RouteDeviationReview decide(RouteDeviationReviewUseCase.Context context, UUID episodeId,
            long expectedVersion, RouteDeviationReview.Status status,
            RouteDeviationReview.Reason reason, String note, String idempotencyKey,
            boolean correction) {
        var current = required(context);
        String key = key(idempotencyKey);
        String requestHash = hash(episodeId + "|" + expectedVersion + "|" + status + "|"
                + reason + "|" + Objects.toString(note, ""));
        return transactions.execute(() -> {
            var claim = support.claim(current.tenantId(), correction ? "REVIEW_CORRECT" : "REVIEW_"
                    + status, key, requestHash, required(episodeId, "Episode ID"),
                    current.actorId(), current.now());
            sameRequest(claim, requestHash);
            if (!claim.acquired()) return reviewAt(current.tenantId(), episodeId,
                    required(claim.resultVersion(), "Review result version"));
            RouteDeviationEpisode episode = episodes.lockAndFind(current.tenantId(), episodeId)
                    .orElseThrow(RouteDeviationManagementService::episodeNotFound);
            version(episode.reviewVersion(), expectedVersion, "ROUTE_DEVIATION_REVIEW_STALE_VERSION");
            if (episode.severity() != RouteDeviationEpisode.Severity.HIGH) throw new ConflictException(
                    "ROUTE_DEVIATION_REVIEW_NOT_REQUIRED", "Only HIGH episodes are reviewable");
            RouteDeviationReview review;
            try {
                if (correction) {
                    RouteDeviationReview prior = reviewAt(current.tenantId(), episodeId, expectedVersion);
                    review = prior.correct(status, reason, note, current.actorId(), current.now(),
                            expectedVersion);
                } else {
                    if (episode.reviewStatus() != RouteDeviationReview.Status.PENDING) {
                        throw new ConflictException("ROUTE_DEVIATION_ALREADY_REVIEWED",
                                "Route-deviation episode was already reviewed");
                    }
                    review = RouteDeviationReview.decide(current.tenantId(), episodeId, status,
                            reason, note, current.actorId(), current.now(), expectedVersion);
                }
            } catch (RouteDeviationException exception) {
                throw conflictOrRule(exception);
            }
            RouteDeviationReview saved = reviews.append(review);
            episodes.save(episode.reviewed(saved));
            support.audit(current.tenantId(), current.actorId(), correction
                    ? "ROUTE_DEVIATION_REVIEW_CORRECTED" : "ROUTE_DEVIATION_REVIEW_" + status,
                    "ROUTE_DEVIATION_EPISODE", episodeId,
                    "STATUS=" + episode.reviewStatus() + "->" + status + ";REASON=" + reason
                            + ";VERSION=" + expectedVersion + "->" + saved.reviewVersion()
                            + ";CORRELATION=" + safe(current.correlationId()), current.now());
            support.complete(claim.claimId(), saved.reviewVersion());
            return saved;
        });
    }

    @Override public Optional<RouteDeviationRule> rule(UUID tenant, UUID id) {
        return rules.findRule(required(tenant, "Tenant ID"), required(id, "Rule ID"));
    }
    @Override public Page<RouteDeviationRule> rules(UUID tenant, RouteDeviationRule.Lifecycle lifecycle,
            int page, int size) {
        page(page, size); UUID t = required(tenant, "Tenant ID");
        return new Page<>(rules.list(t, lifecycle, page * size, size), page, size,
                rules.count(t, lifecycle));
    }
    @Override public Optional<VehicleRouteDeviationState> state(UUID tenant, UUID vehicle) {
        return states.find(required(tenant, "Tenant ID"), required(vehicle, "Vehicle ID"));
    }
    @Override public Page<VehicleRouteDeviationState> states(UUID tenant,
            VehicleRouteDeviationState.State state, int page, int size) {
        page(page, size); UUID t = required(tenant, "Tenant ID");
        return new Page<>(states.list(t, state, page * size, size), page, size,
                states.count(t, state));
    }
    @Override public Optional<RouteDeviationEpisode> episode(UUID tenant, UUID id) {
        return episodes.find(required(tenant, "Tenant ID"), required(id, "Episode ID"));
    }
    @Override public CursorPage<RouteDeviationEpisode> episodes(UUID tenant, UUID vehicle,
            UUID trip, UUID route, RouteDeviationEpisode.Severity severity, Boolean open,
            Instant from, Instant to, String cursor, int limit) {
        range(from, to, limit);
        Cursor after = cursor(cursor);
        List<RouteDeviationEpisode> found = episodes.search(required(tenant, "Tenant ID"), vehicle,
                trip, route, severity, open, from, to, after.time(), after.id(), limit + 1);
        boolean more = found.size() > limit;
        List<RouteDeviationEpisode> items = more ? List.copyOf(found.subList(0, limit)) : found;
        return new CursorPage<>(items, more ? cursor(items.getLast()) : null);
    }
    @Override public List<RouteDeviationReview> reviews(UUID tenant, UUID episode, int limit) {
        if (limit < 1 || limit > 100) throw rule("ROUTE_DEVIATION_REVIEW_PAGE_INVALID",
                "Review limit must be between 1 and 100");
        if (episodes.find(required(tenant, "Tenant ID"), required(episode, "Episode ID")).isEmpty()) {
            throw episodeNotFound();
        }
        List<RouteDeviationReview> found = reviews.history(tenant, episode);
        return found.size() <= limit ? found : List.copyOf(found.subList(0, limit));
    }

    private RouteDeviationReview reviewAt(UUID tenant, UUID episode, long version) {
        return reviews.history(tenant, episode).stream().filter(r -> r.reviewVersion() == version)
                .findFirst().orElseThrow(() -> new ConflictException(
                        "ROUTE_DEVIATION_REVIEW_VERSION_MISSING", "Review version is unavailable"));
    }
    private RouteDeviationRule requiredRule(UUID tenant, UUID id) {
        return rules.findRule(tenant, id).orElseThrow(() -> new NotFoundException(
                "ROUTE_DEVIATION_RULE_NOT_FOUND", "Route-deviation rule not found"));
    }
    private RouteDeviationRule save(RouteDeviationRule rule) {
        try { return rules.save(rule); }
        catch (IllegalStateException exception) { throw new ConflictException(
                "ROUTE_DEVIATION_RULE_STALE_VERSION", "Route-deviation rule version is stale", exception); }
    }
    private static RouteDeviationRuleManagementUseCase.Context required(
            RouteDeviationRuleManagementUseCase.Context value) {
        Objects.requireNonNull(value, "Rule context is required");
        required(value.tenantId(), "Tenant ID"); required(value.actorId(), "Actor ID");
        required(value.now(), "Current time"); return value;
    }
    private static RouteDeviationReviewUseCase.Context required(RouteDeviationReviewUseCase.Context value) {
        Objects.requireNonNull(value, "Review context is required");
        required(value.tenantId(), "Tenant ID"); required(value.actorId(), "Actor ID");
        required(value.now(), "Current time"); return value;
    }
    private static void page(int page, int size) { if (page < 0 || size < 1 || size > 100)
        throw rule("ROUTE_DEVIATION_PAGE_INVALID", "Page must be non-negative and size between 1 and 100"); }
    private static void range(Instant from, Instant to, int limit) {
        required(from, "From source time"); required(to, "To source time");
        if (from.isAfter(to) || Duration.between(from, to).compareTo(MAX_RANGE) > 0)
            throw rule("ROUTE_DEVIATION_RANGE_INVALID", "Range must be ordered and no greater than 31 days");
        if (limit < 1 || limit > 500) throw rule("ROUTE_DEVIATION_EPISODE_PAGE_INVALID",
                "Episode limit must be between 1 and 500");
    }
    private static String key(String value) { if (value == null || value.isBlank()
            || value.trim().length() > 160) throw rule("IDEMPOTENCY_KEY_INVALID",
                    "Idempotency-Key must contain 1 to 160 characters"); return value.trim(); }
    private static String reason(String value) { if (value == null || value.isBlank()
            || value.trim().length() > 300) throw rule("ROUTE_DEVIATION_REASON_INVALID",
                    "Reason must contain 1 to 300 characters"); return value.trim(); }
    private static void version(long actual, long expected, String code) {
        if (expected < 0 || actual != expected) throw new ConflictException(code, "Version is stale");
    }
    private static void sameRequest(RouteDeviationManagementSupportPort.Claim claim, String hash) {
        if (!claim.requestHash().equals(hash)) throw new ConflictException("IDEMPOTENCY_KEY_CONFLICT",
                "Idempotency-Key was already used for another request");
    }
    private static String detail(RouteDeviationRule before, RouteDeviationRule after) {
        return "LIFECYCLE=" + (before == null ? "NONE" : before.lifecycle()) + "->" + after.lifecycle()
                + ";MANAGE_VERSION=" + (before == null ? "NONE" : before.manageVersion()) + "->"
                + after.manageVersion() + ";RULE_VERSION=" + (before == null ? "NONE" : before.ruleVersion())
                + "->" + after.ruleVersion() + ";ROUTE_ID=" + after.routeId() + ";ROUTE_VERSION="
                + after.routeVersion().value() + ";TOLERANCE_METERS=" + after.configuredTolerance().value();
    }
    private static String safe(String value) { return value == null ? "NONE"
            : value.replaceAll("[^A-Za-z0-9._:-]", "_").substring(0, Math.min(value.length(), 100)); }
    private static String hash(String value) { try { return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); } }
    private static String cursor(RouteDeviationEpisode e) { return Base64.getUrlEncoder().withoutPadding()
            .encodeToString((e.startSourceTimestamp() + "|" + e.id()).getBytes(StandardCharsets.UTF_8)); }
    private static Cursor cursor(String value) { if (value == null || value.isBlank()) return new Cursor(null,null);
        try { String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int split = decoded.lastIndexOf('|'); return new Cursor(Instant.parse(decoded.substring(0,split)),
                    UUID.fromString(decoded.substring(split+1))); }
        catch (RuntimeException exception) { throw rule("ROUTE_DEVIATION_CURSOR_INVALID", "Cursor is invalid"); } }
    private static RuntimeException conflictOrRule(RouteDeviationException exception) {
        if (exception.code().contains("STALE") || exception.code().contains("TRANSITION")
                || exception.code().contains("EDITABLE") || exception.code().contains("FORBIDDEN"))
            return new ConflictException(exception.code(), exception.getMessage(), exception);
        return new BusinessRuleException(exception.code(), exception.getMessage());
    }
    private static BusinessRuleException rule(String code, String message) {
        return new BusinessRuleException(code, message);
    }
    private static NotFoundException episodeNotFound() { return new NotFoundException(
            "ROUTE_DEVIATION_EPISODE_NOT_FOUND", "Route-deviation episode not found"); }
    private static <T> T required(T value, String name) { return Objects.requireNonNull(value,
            name + " is required"); }
    private record Cursor(Instant time, UUID id) { }
    @FunctionalInterface private interface RuleMutation { RouteDeviationRule apply(RouteDeviationRule rule); }
}
