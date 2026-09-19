package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tracking.ports.inbound.IdleMonitoringQuery;
import com.transportlogistics.app.tracking.ports.outbound.IdleMonitoringAuditPort;
import com.transportlogistics.app.tracking.ports.outbound.IdleMonitoringCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.IdleMonitoringReadPort;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class IdleMonitoringQueryService implements IdleMonitoringQuery {
    private static final Duration MAX_RANGE = Duration.ofDays(31);
    private static final Set<String> STATES = Set.of("UNKNOWN","UNSUPPORTED","NOT_REPORTED","STALE",
            "CONFLICTING_EVIDENCE","CANDIDATE","IDLE","NORMAL");
    private static final Set<String> END_REASONS = Set.of("ENGINE_STOPPED","MOVEMENT","EVIDENCE_GAP",
            "DEVICE_REASSIGNED","CAPABILITY_CHANGED");
    private final IdleMonitoringReadPort reads;
    private final IdleMonitoringCursorPort cursors;
    private final IdleMonitoringAuditPort audit;

    public IdleMonitoringQueryService(IdleMonitoringReadPort reads, IdleMonitoringCursorPort cursors,
            IdleMonitoringAuditPort audit) {
        this.reads = reads; this.cursors = cursors; this.audit = audit;
    }

    @Override public Page<State> states(Context context, StateFilter filter, String cursor, int limit) {
        required(context); bounds(limit); StateFilter actual = filter == null ? new StateFilter(null, null) : filter;
        if(actual.state()!=null && !STATES.contains(actual.state()))
            throw new BusinessRuleException("IDLE_MONITOR_STATE_INVALID","Idle monitoring state is invalid");
        String binding = "STATES|" + actual.vehicleId() + "|" + actual.state();
        var after = decode(context, binding, cursor);
        List<State> rows = reads.states(context.tenantId(), actual, timestamp(after), id(after), limit + 1);
        Page<State> page = page(context, binding, rows, limit, value -> value.latestSourceTimestamp(), value -> value.vehicleId());
        audit.record(context.tenantId(), context.actorId(), context.correlationId(), "IDLE_MONITOR_STATES_VIEWED",
                actual.vehicleId(), shape(actual), limit, page.items().size(), context.now());
        return page;
    }

    @Override public Page<Episode> episodes(Context context, EpisodeFilter filter, String cursor, int limit) {
        required(context); validate(filter, limit); String binding = "EPISODES|" + filter.vehicleId() + "|"
                + filter.from() + "|" + filter.to() + "|" + filter.endReason();
        var after = decode(context, binding, cursor);
        List<Episode> rows = reads.episodes(context.tenantId(), filter, timestamp(after), id(after), limit + 1);
        Page<Episode> page = page(context, binding, rows, limit, Episode::startSourceTimestamp, Episode::id);
        audit.record(context.tenantId(), context.actorId(), context.correlationId(), "IDLE_EPISODES_VIEWED",
                filter.vehicleId(), shape(filter), limit, page.items().size(), context.now());
        return page;
    }

    @Override public Optional<Episode> episode(Context context, UUID episodeId) {
        required(context); UUID id = Objects.requireNonNull(episodeId, "episodeId");
        Optional<Episode> result = reads.episode(context.tenantId(), id);
        audit.record(context.tenantId(), context.actorId(), context.correlationId(), "IDLE_EPISODE_VIEWED",
                id, "DETAIL", 1, result.isPresent() ? 1 : 0, context.now());
        return result;
    }

    @Override public Page<Evidence> evidence(Context context, UUID episodeId, String cursor, int limit) {
        required(context); bounds(limit); UUID id = Objects.requireNonNull(episodeId, "episodeId");
        if (reads.episode(context.tenantId(), id).isEmpty()) throw notFound();
        String binding = "EVIDENCE|" + id; var after = decode(context, binding, cursor);
        List<Evidence> rows = reads.evidence(context.tenantId(), id, timestamp(after), id(after), limit + 1);
        Page<Evidence> page = page(context, binding, rows, limit, Evidence::sourceTimestamp, Evidence::id);
        audit.record(context.tenantId(), context.actorId(), context.correlationId(), "IDLE_EVIDENCE_VIEWED",
                id, "EVIDENCE", limit, page.items().size(), context.now());
        return page;
    }

    private <T> Page<T> page(Context c, String binding, List<T> rows, int limit,
            java.util.function.Function<T, java.time.Instant> time,
            java.util.function.Function<T, UUID> identity) {
        boolean more = rows.size() > limit;
        List<T> items = List.copyOf(rows.subList(0, Math.min(limit, rows.size())));
        String next = more ? cursors.encode(c.tenantId(), binding, time.apply(items.getLast()), identity.apply(items.getLast())) : null;
        return new Page<>(items, next);
    }
    private IdleMonitoringCursorPort.Position decode(Context c, String binding, String cursor) {
        return cursor == null ? null : cursors.decode(c.tenantId(), binding, cursor);
    }
    private static java.time.Instant timestamp(IdleMonitoringCursorPort.Position p) { return p == null ? null : p.timestamp(); }
    private static UUID id(IdleMonitoringCursorPort.Position p) { return p == null ? null : p.id(); }
    private static void validate(EpisodeFilter f, int limit) {
        bounds(limit);
        if (f == null || f.from() == null || f.to() == null || !f.from().isBefore(f.to())
                || Duration.between(f.from(), f.to()).compareTo(MAX_RANGE) > 0)
            throw new BusinessRuleException("IDLE_EPISODE_RANGE_INVALID", "Range must be positive and at most 31 days");
        if(f.endReason()!=null && !END_REASONS.contains(f.endReason()))
            throw new BusinessRuleException("IDLE_EPISODE_END_REASON_INVALID","Idle episode end reason is invalid");
    }
    private static void bounds(int limit) {
        if (limit < 1 || limit > 100)
            throw new BusinessRuleException("IDLE_MONITOR_LIMIT_INVALID", "Limit must be 1..100");
    }
    private static void required(Context c) {
        Objects.requireNonNull(c); Objects.requireNonNull(c.tenantId()); Objects.requireNonNull(c.actorId()); Objects.requireNonNull(c.now());
    }
    private static String shape(StateFilter f) { return "vehicle=" + (f.vehicleId()!=null) + ";state=" + (f.state()!=null); }
    private static String shape(EpisodeFilter f) { return "vehicle=" + (f.vehicleId()!=null) + ";range=true;endReason=" + (f.endReason()!=null); }
    private static NotFoundException notFound() { return new NotFoundException("IDLE_EPISODE_NOT_FOUND", "Idle episode not found"); }
}
