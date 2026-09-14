package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationReview;
import java.time.Instant;
import java.util.UUID;

public interface RouteDeviationReviewUseCase {
    RouteDeviationReview approve(Context context, UUID episodeId, long expectedVersion,
                                 RouteDeviationReview.Reason reason, String note,
                                 String idempotencyKey);
    RouteDeviationReview reject(Context context, UUID episodeId, long expectedVersion,
                                RouteDeviationReview.Reason reason, String note,
                                String idempotencyKey);
    RouteDeviationReview correct(Context context, UUID episodeId, long expectedVersion,
                                 RouteDeviationReview.Status status,
                                 RouteDeviationReview.Reason reason, String note,
                                 String idempotencyKey);
    record Context(UUID tenantId, UUID actorId, String correlationId, Instant now) { }
}
