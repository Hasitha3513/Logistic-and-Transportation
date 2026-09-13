package com.transportlogistics.app.tracking.ports.outbound;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationReview;import java.util.*;
public interface RouteDeviationReviewRepositoryPort {RouteDeviationReview append(RouteDeviationReview review);List<RouteDeviationReview> history(UUID tenantId,UUID episodeId);}
