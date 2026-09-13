package com.transportlogistics.app.tracking.adapters.outbound.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.LiveTelemetryProjection;
import com.transportlogistics.app.tracking.ports.outbound.LiveTelemetryProjectionPort;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public final class RedisLiveTelemetryProjectionAdapter implements LiveTelemetryProjectionPort {
    static final String LIVE_PREFIX = "tracking:live:";
    static final String INDEX_PREFIX = "tracking:live-index:";
    private static final DefaultRedisScript<Long> PROJECT = new DefaultRedisScript<>("""
            local oldSecond = redis.call('HGET', KEYS[1], 'sourceSecond')
            local oldNano = redis.call('HGET', KEYS[1], 'sourceNano')
            local oldEvent = redis.call('HGET', KEYS[1], 'eventId')
            local incomingSecond = ARGV[1]
            local incomingNano = ARGV[2]
            local incomingEvent = ARGV[3]
            local result = 0
            if not oldSecond or tonumber(incomingSecond) > tonumber(oldSecond)
               or (tonumber(incomingSecond) == tonumber(oldSecond) and tonumber(incomingNano) > tonumber(oldNano))
               or (tonumber(incomingSecond) == tonumber(oldSecond) and tonumber(incomingNano) == tonumber(oldNano)
                   and incomingEvent > oldEvent) then
              redis.call('HSET', KEYS[1], 'sourceSecond', incomingSecond, 'sourceNano', incomingNano,
                         'eventId', incomingEvent, 'projection', ARGV[4])
              result = 1
            elseif incomingEvent == oldEvent then
              result = 2
            end
            if result > 0 then
              redis.call('PEXPIRE', KEYS[1], ARGV[5])
              redis.call('ZADD', KEYS[2], ARGV[6], ARGV[7])
              redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[8])
              local excess = redis.call('ZCARD', KEYS[2]) - tonumber(ARGV[9])
              if excess > 0 then redis.call('ZREMRANGEBYRANK', KEYS[2], 0, excess - 1) end
              redis.call('PEXPIRE', KEYS[2], ARGV[5])
            end
            return result
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final Duration ttl;
    private final int maximumIndexSize;

    public RedisLiveTelemetryProjectionAdapter(
            StringRedisTemplate redis,
            ObjectMapper json,
            @Value("${app.tracking.hybrid-storage.live-state-ttl:PT24H}") Duration ttl,
            @Value("${app.tracking.hybrid-storage.maximum-live-index-size:10000}")
                    int maximumIndexSize) {
        this.redis = redis;
        this.json = json;
        this.ttl = ttl;
        this.maximumIndexSize = maximumIndexSize;
        if (!Duration.ofHours(24).equals(ttl) || maximumIndexSize < 1 || maximumIndexSize > 10_000) {
            throw new IllegalArgumentException("Invalid Tracking live projection bounds");
        }
    }

    @Override
    public ProjectionResult project(LiveTelemetryProjection projection) {
        var event = projection.telemetry();
        String stateKey = liveKey(event.tenantId(), event.vehicleId());
        String indexKey = indexKey(event.tenantId());
        long now = projection.projectedAt().toEpochMilli();
        long expiresAt = Math.addExact(now, ttl.toMillis());
        try {
            Long result = redis.execute(PROJECT, List.of(stateKey, indexKey),
                    Long.toString(event.recordedAt().getEpochSecond()),
                    Integer.toString(event.recordedAt().getNano()), event.eventId().toString(),
                    json.writeValueAsString(projection), Long.toString(ttl.toMillis()),
                    Long.toString(expiresAt), event.vehicleId().toString(), Long.toString(now),
                    Integer.toString(maximumIndexSize));
            if (result == null) {
                throw unavailable(null);
            }
            return result == 1 ? ProjectionResult.UPDATED
                    : result == 2 ? ProjectionResult.DUPLICATE : ProjectionResult.STALE;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Canonical live projection cannot be serialized", exception);
        } catch (RuntimeException exception) {
            if (exception instanceof DependencyUnavailableException) {
                throw exception;
            }
            throw unavailable(exception);
        }
    }

    @Override
    public Optional<LiveTelemetryProjection> find(UUID tenantId, UUID vehicleId) {
        try {
            Object value = redis.opsForHash().get(liveKey(tenantId, vehicleId), "projection");
            return value == null ? Optional.empty() : Optional.of(read(value.toString()));
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public List<LiveTelemetryProjection> findLive(UUID tenantId, Instant now, int limit) {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("Live projection limit must be 1..500");
        }
        try {
            String index = indexKey(tenantId);
            redis.opsForZSet().removeRangeByScore(index, Double.NEGATIVE_INFINITY, now.toEpochMilli());
            var members = redis.opsForZSet().reverseRangeByScore(
                    index, now.toEpochMilli() + 1D, Double.POSITIVE_INFINITY, 0, limit);
            if (members == null || members.isEmpty()) {
                return List.of();
            }
            List<LiveTelemetryProjection> result = new ArrayList<>(members.size());
            List<String> missing = new ArrayList<>();
            for (String member : members) {
                UUID vehicleId = UUID.fromString(member);
                var state = find(tenantId, vehicleId);
                if (state.isPresent()) {
                    result.add(state.get());
                } else {
                    missing.add(member);
                }
            }
            if (!missing.isEmpty()) {
                redis.opsForZSet().remove(index, missing.toArray());
            }
            return List.copyOf(result);
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalArgumentException
                    || exception instanceof DependencyUnavailableException) {
                throw exception;
            }
            throw unavailable(exception);
        }
    }

    static String liveKey(UUID tenantId, UUID vehicleId) {
        return LIVE_PREFIX + "{" + tenantId + "}:" + vehicleId;
    }

    static String indexKey(UUID tenantId) {
        return INDEX_PREFIX + "{" + tenantId + "}";
    }

    private LiveTelemetryProjection read(String value) {
        try {
            return json.readValue(value, LiveTelemetryProjection.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored live projection is invalid", exception);
        }
    }

    private static DependencyUnavailableException unavailable(Throwable cause) {
        return new DependencyUnavailableException(
                "TRACKING_LIVE_PROJECTION_UNAVAILABLE", "Live telemetry projection is unavailable", cause);
    }
}
