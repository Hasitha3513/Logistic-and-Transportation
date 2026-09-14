package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface RouteDeviationRuleManagementUseCase {
    RouteDeviationRule create(Context context, UUID routeId, String routeVersion,
                              BigDecimal toleranceMeters, String idempotencyKey);
    RouteDeviationRule update(Context context, UUID id, long expectedVersion,
                              BigDecimal toleranceMeters, String idempotencyKey);
    RouteDeviationRule activate(Context context, UUID id, long expectedVersion, String idempotencyKey);
    RouteDeviationRule disable(Context context, UUID id, long expectedVersion,
                               String reason, String idempotencyKey);
    RouteDeviationRule retire(Context context, UUID id, long expectedVersion,
                              String reason, String idempotencyKey);
    record Context(UUID tenantId, UUID actorId, String correlationId, Instant now) { }
}
