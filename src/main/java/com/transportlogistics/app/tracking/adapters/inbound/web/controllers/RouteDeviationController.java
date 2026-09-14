package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.request.RouteDeviationRequests;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.RouteDeviationResponses;
import com.transportlogistics.app.tracking.adapters.inbound.web.mappers.RouteDeviationWebMapper;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationQueryUseCase;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationReviewUseCase;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationRuleManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/tracking/route-deviations")
@Tag(name = "Route Deviations")
public class RouteDeviationController {
    private final RouteDeviationRuleManagementUseCase management;
    private final RouteDeviationReviewUseCase review;
    private final RouteDeviationQueryUseCase query;
    private final RouteDeviationWebMapper mapper;
    private final CurrentTenant currentTenant;

    public RouteDeviationController(RouteDeviationRuleManagementUseCase management,
            RouteDeviationReviewUseCase review, RouteDeviationQueryUseCase query,
            RouteDeviationWebMapper mapper, CurrentTenant currentTenant) {
        this.management = management;
        this.review = review;
        this.query = query;
        this.mapper = mapper;
        this.currentTenant = currentTenant;
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_MANAGE')")
    @Operation(summary = "Create a route-deviation rule")
    public RouteDeviationResponses.Rule create(@RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody RouteDeviationRequests.CreateRule request) {
        return mapper.response(management.create(ruleContext(), request.routeId(),
                request.routeVersion(), request.toleranceMeters(), key));
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_VIEW')")
    @Operation(summary = "List route-deviation rules")
    public RouteDeviationResponses.RulePage rules(
            @RequestParam(required = false) RouteDeviationRule.Lifecycle lifecycle,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.response(query.rules(tenant(), lifecycle, page, size));
    }

    @GetMapping("/rules/{ruleId}")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_VIEW')")
    @Operation(summary = "Get a route-deviation rule")
    public RouteDeviationResponses.Rule rule(@PathVariable UUID ruleId) {
        return mapper.response(query.rule(tenant(), ruleId).orElseThrow(
                RouteDeviationController::ruleNotFound));
    }

    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_MANAGE')")
    @Operation(summary = "Update a draft route-deviation rule")
    public RouteDeviationResponses.Rule update(@PathVariable UUID ruleId,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody RouteDeviationRequests.UpdateRule request) {
        return mapper.response(management.update(ruleContext(), ruleId,
                request.expectedVersion(), request.toleranceMeters(), key));
    }

    @PostMapping("/rules/{ruleId}/activate")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_MANAGE')")
    @Operation(summary = "Activate a route-deviation rule")
    public RouteDeviationResponses.Rule activate(@PathVariable UUID ruleId,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody RouteDeviationRequests.Version request) {
        return mapper.response(management.activate(ruleContext(), ruleId,
                request.expectedVersion(), key));
    }

    @PostMapping("/rules/{ruleId}/disable")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_MANAGE')")
    @Operation(summary = "Disable a route-deviation rule")
    public RouteDeviationResponses.Rule disable(@PathVariable UUID ruleId,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody RouteDeviationRequests.Lifecycle request) {
        return mapper.response(management.disable(ruleContext(), ruleId,
                request.expectedVersion(), request.reason(), key));
    }

    @PostMapping("/rules/{ruleId}/retire")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_MANAGE')")
    @Operation(summary = "Retire a route-deviation rule")
    public RouteDeviationResponses.Rule retire(@PathVariable UUID ruleId,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody RouteDeviationRequests.Lifecycle request) {
        return mapper.response(management.retire(ruleContext(), ruleId,
                request.expectedVersion(), request.reason(), key));
    }

    @GetMapping("/states")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_VIEW')")
    @Operation(summary = "List current vehicle route-deviation states")
    public RouteDeviationResponses.StatePage states(
            @RequestParam(required = false) VehicleRouteDeviationState.State state,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.stateResponse(query.states(tenant(), state, page, size));
    }

    @GetMapping("/states/{vehicleId}")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_VIEW')")
    @Operation(summary = "Get current route-deviation state for a vehicle")
    public RouteDeviationResponses.State state(@PathVariable UUID vehicleId) {
        return mapper.response(query.state(tenant(), vehicleId).orElseThrow(
                RouteDeviationController::stateNotFound));
    }

    @GetMapping("/episodes")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_EVENT_VIEW')")
    @Operation(summary = "Search route-deviation episodes by source time")
    public RouteDeviationResponses.EpisodePage episodes(
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) UUID tripId,
            @RequestParam(required = false) UUID routeId,
            @RequestParam(required = false) RouteDeviationEpisode.Severity severity,
            @RequestParam(required = false) Boolean open,
            @RequestParam @NotNull Instant from, @RequestParam @NotNull Instant to,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return mapper.response(query.episodes(tenant(), vehicleId, tripId, routeId, severity,
                open, from, to, cursor, limit));
    }

    @GetMapping("/episodes/{episodeId}")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_EVENT_VIEW')")
    @Operation(summary = "Get a route-deviation episode")
    public RouteDeviationResponses.Episode episode(@PathVariable UUID episodeId) {
        return mapper.response(query.episode(tenant(), episodeId).orElseThrow(
                RouteDeviationController::episodeNotFound));
    }

    @GetMapping("/episodes/{episodeId}/reviews")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_EVENT_VIEW')")
    @Operation(summary = "List immutable review history")
    public RouteDeviationResponses.ReviewPage reviews(@PathVariable UUID episodeId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit) {
        return new RouteDeviationResponses.ReviewPage(query.reviews(tenant(), episodeId, limit)
                .stream().map(mapper::response).toList());
    }

    @PostMapping("/episodes/{episodeId}/approve")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_APPROVE')")
    @Operation(summary = "Approve a high-severity route-deviation episode")
    public RouteDeviationResponses.Review approve(@PathVariable UUID episodeId,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody RouteDeviationRequests.Review request) {
        return mapper.response(review.approve(reviewContext(), episodeId,
                request.expectedVersion(), request.reason(), request.note(), key));
    }

    @PostMapping("/episodes/{episodeId}/reject")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_APPROVE')")
    @Operation(summary = "Reject a high-severity route-deviation episode")
    public RouteDeviationResponses.Review reject(@PathVariable UUID episodeId,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody RouteDeviationRequests.Review request) {
        return mapper.response(review.reject(reviewContext(), episodeId,
                request.expectedVersion(), request.reason(), request.note(), key));
    }

    @PostMapping("/episodes/{episodeId}/correct-review")
    @PreAuthorize("hasAuthority('ROUTE_DEVIATION_APPROVE')")
    @Operation(summary = "Append a compensating correction to a prior review")
    public RouteDeviationResponses.Review correct(@PathVariable UUID episodeId,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody RouteDeviationRequests.CorrectReview request) {
        return mapper.response(review.correct(reviewContext(), episodeId,
                request.expectedVersion(), request.status(), request.reason(), request.note(), key));
    }

    private UUID tenant() {
        return currentTenant.required().tenantId();
    }

    private RouteDeviationRuleManagementUseCase.Context ruleContext() {
        var context = currentTenant.required();
        return new RouteDeviationRuleManagementUseCase.Context(context.tenantId(),
                context.actorId(), context.correlationId(), Instant.now());
    }

    private RouteDeviationReviewUseCase.Context reviewContext() {
        var context = currentTenant.required();
        return new RouteDeviationReviewUseCase.Context(context.tenantId(), context.actorId(),
                context.correlationId(), Instant.now());
    }

    private static NotFoundException ruleNotFound() {
        return new NotFoundException("ROUTE_DEVIATION_RULE_NOT_FOUND",
                "Route-deviation rule not found");
    }

    private static NotFoundException stateNotFound() {
        return new NotFoundException("ROUTE_DEVIATION_STATE_NOT_FOUND",
                "Route-deviation state not found");
    }

    private static NotFoundException episodeNotFound() {
        return new NotFoundException("ROUTE_DEVIATION_EPISODE_NOT_FOUND",
                "Route-deviation episode not found");
    }
}
