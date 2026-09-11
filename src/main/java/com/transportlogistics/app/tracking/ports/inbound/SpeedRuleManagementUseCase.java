package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import java.time.Instant;
import java.util.UUID;

public interface SpeedRuleManagementUseCase {
    SpeedRule create(Context context, CreateRule command, String idempotencyKey);
    SpeedRule update(Context context, UUID ruleId, long expectedVersion, UpdateRule command);
    SpeedRule activate(Context context, UUID ruleId, long expectedVersion, String idempotencyKey);
    SpeedRule disable(Context context, UUID ruleId, long expectedVersion, String reason, String idempotencyKey);
    SpeedRule retire(Context context, UUID ruleId, long expectedVersion, String reason, String idempotencyKey);

    record Context(UUID tenantId, UUID actorId, String correlationId, Instant now) {
    }

    record CreateRule(String name, SpeedRule.Scope scope, UUID routeId,
                      String routeVersion, SpeedKph thresholdKph) {
    }

    record UpdateRule(String name, UUID routeId, String routeVersion, SpeedKph thresholdKph) {
    }
}
