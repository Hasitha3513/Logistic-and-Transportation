package com.transportlogistics.app.tracking.adapters.inbound.web.mappers;

import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.RouteDeviationResponses;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationReview;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationQueryUseCase;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RouteDeviationWebMapper {
    default RouteDeviationResponses.Rule response(RouteDeviationRule value) {
        return new RouteDeviationResponses.Rule(value.id(), value.routeId(),
                value.routeVersion().value(), value.configuredTolerance().value(),
                value.lifecycle().name(), value.ruleVersion(), value.manageVersion(), value.effectiveAt());
    }
    default RouteDeviationResponses.State response(VehicleRouteDeviationState value) {
        return new RouteDeviationResponses.State(value.vehicleId(), value.state().name(),
                value.availability().name(), value.currentTripId(), value.routeId(),
                value.routeVersion() == null ? null : value.routeVersion().value(), value.ruleId(),
                value.ruleVersion(), value.activeEpisodeId(), value.lastSourceTimestamp());
    }
    default RouteDeviationResponses.Episode response(RouteDeviationEpisode value) {
        return new RouteDeviationResponses.Episode(value.id(), value.vehicleId(), value.tripId(),
                value.driverId(), value.routeId(), value.routeVersion().value(), value.ruleId(),
                value.ruleVersion(), value.configuredTolerance().value(),
                value.effectiveTolerance().value(), value.firstCandidatePositionId(),
                value.confirmingPositionId(), value.startSourceTimestamp(),
                value.confirmationSourceTimestamp(), value.endSourceTimestamp(),
                value.maximumDistance().value(), value.eligibleOutsideSampleCount(),
                value.severity().name(), value.reviewStatus().name(), value.reviewVersion(),
                value.terminalOutcome() == null ? null : value.terminalOutcome().name(),
                value.disruptionId());
    }
    default RouteDeviationResponses.Review response(RouteDeviationReview value) {
        return new RouteDeviationResponses.Review(value.id(), value.episodeId(), value.status().name(),
                value.reason().name(), value.note(), value.reviewerId(), value.reviewedAt(),
                value.reviewVersion(), value.compensatesReviewId());
    }
    default RouteDeviationResponses.RulePage response(RouteDeviationQueryUseCase.Page<RouteDeviationRule> page) {
        return new RouteDeviationResponses.RulePage(page.items().stream().map(this::response).toList(),
                page.page(), page.size(), page.total());
    }
    default RouteDeviationResponses.StatePage stateResponse(
            RouteDeviationQueryUseCase.Page<VehicleRouteDeviationState> page) {
        return new RouteDeviationResponses.StatePage(page.items().stream().map(this::response).toList(),
                page.page(), page.size(), page.total());
    }
    default RouteDeviationResponses.EpisodePage response(
            RouteDeviationQueryUseCase.CursorPage<RouteDeviationEpisode> page) {
        return new RouteDeviationResponses.EpisodePage(
                page.items().stream().map(this::response).toList(), page.nextCursor());
    }
}
