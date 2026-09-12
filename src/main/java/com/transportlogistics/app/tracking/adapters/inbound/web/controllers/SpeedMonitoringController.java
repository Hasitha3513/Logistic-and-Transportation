package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.request.SpeedMonitoringRequests;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.SpeedMonitoringResponses;
import com.transportlogistics.app.tracking.adapters.inbound.web.mappers.SpeedMonitoringWebMapper;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.domain.speed.VehicleSpeedState;
import com.transportlogistics.app.tracking.ports.inbound.SpeedMonitoringQuery;
import com.transportlogistics.app.tracking.ports.inbound.SpeedRuleManagementUseCase;
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
@RequestMapping("/v1/tracking/speed-monitoring")
public class SpeedMonitoringController {
    private final SpeedRuleManagementUseCase management;
    private final SpeedMonitoringQuery query;
    private final SpeedMonitoringWebMapper mapper;
    private final CurrentTenant currentTenant;
    public SpeedMonitoringController(SpeedRuleManagementUseCase management, SpeedMonitoringQuery query,
                                     SpeedMonitoringWebMapper mapper, CurrentTenant currentTenant) {
        this.management=management; this.query=query; this.mapper=mapper; this.currentTenant=currentTenant;
    }
    @PostMapping("/rules") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('SPEED_MONITOR_MANAGE')")
    public SpeedMonitoringResponses.Rule create(@RequestHeader("Idempotency-Key") String key,
                                                 @Valid @RequestBody SpeedMonitoringRequests.Create request) {
        return mapper.response(management.create(context(),mapper.command(request),key));
    }
    @GetMapping("/rules") @PreAuthorize("hasAuthority('SPEED_MONITOR_VIEW')")
    public SpeedMonitoringResponses.RulePage rules(@RequestParam(required=false) SpeedRule.Scope scope,
                                                    @RequestParam(required=false) SpeedRule.Lifecycle lifecycle,
                                                    @RequestParam(defaultValue="0") @Min(0) int page,
                                                    @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return mapper.rules(query.rules(tenant(),scope,lifecycle,page,size));
    }
    @GetMapping("/rules/{ruleId}") @PreAuthorize("hasAuthority('SPEED_MONITOR_VIEW')")
    public SpeedMonitoringResponses.Rule rule(@PathVariable UUID ruleId) {
        return mapper.response(query.rule(tenant(),ruleId).orElseThrow(SpeedMonitoringController::ruleNotFound));
    }
    @PutMapping("/rules/{ruleId}") @PreAuthorize("hasAuthority('SPEED_MONITOR_MANAGE')")
    public SpeedMonitoringResponses.Rule update(@PathVariable UUID ruleId,
                                                 @Valid @RequestBody SpeedMonitoringRequests.Update request) {
        return mapper.response(management.update(context(),ruleId,request.expectedVersion(),mapper.command(request)));
    }
    @PostMapping("/rules/{ruleId}/activate") @PreAuthorize("hasAuthority('SPEED_MONITOR_MANAGE')")
    public SpeedMonitoringResponses.Rule activate(@PathVariable UUID ruleId,
                                                   @RequestHeader("Idempotency-Key") String key,
                                                   @Valid @RequestBody SpeedMonitoringRequests.Version request) {
        return mapper.response(management.activate(context(),ruleId,request.expectedVersion(),key));
    }
    @PostMapping("/rules/{ruleId}/disable") @PreAuthorize("hasAuthority('SPEED_MONITOR_MANAGE')")
    public SpeedMonitoringResponses.Rule disable(@PathVariable UUID ruleId,
                                                  @RequestHeader("Idempotency-Key") String key,
                                                  @Valid @RequestBody SpeedMonitoringRequests.Reason request) {
        return mapper.response(management.disable(context(),ruleId,request.expectedVersion(),request.reason(),key));
    }
    @PostMapping("/rules/{ruleId}/retire") @PreAuthorize("hasAuthority('SPEED_MONITOR_MANAGE')")
    public SpeedMonitoringResponses.Rule retire(@PathVariable UUID ruleId,
                                                 @RequestHeader("Idempotency-Key") String key,
                                                 @Valid @RequestBody SpeedMonitoringRequests.Reason request) {
        return mapper.response(management.retire(context(),ruleId,request.expectedVersion(),request.reason(),key));
    }
    @GetMapping("/states") @PreAuthorize("hasAuthority('SPEED_MONITOR_VIEW')")
    public SpeedMonitoringResponses.StatePage states(@RequestParam(required=false) VehicleSpeedState.MonitoringState state,
                                                      @RequestParam(defaultValue="0") @Min(0) int page,
                                                      @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return mapper.states(query.states(tenant(),state,page,size));
    }
    @GetMapping("/states/{vehicleId}") @PreAuthorize("hasAuthority('SPEED_MONITOR_VIEW')")
    public SpeedMonitoringResponses.State state(@PathVariable UUID vehicleId) {
        return mapper.response(query.state(tenant(),vehicleId).orElseThrow(SpeedMonitoringController::stateNotFound));
    }
    @GetMapping("/episodes") @PreAuthorize("hasAuthority('SPEED_EVENT_VIEW')")
    public SpeedMonitoringResponses.EpisodePage episodes(@RequestParam(required=false) UUID vehicleId,
                                                          @RequestParam(required=false) UUID driverId,
                                                          @RequestParam @NotNull Instant from,
                                                          @RequestParam @NotNull Instant to,
                                                          @RequestParam(required=false) String cursor,
                                                          @RequestParam(defaultValue="100") @Min(1) @Max(500) int limit) {
        return mapper.episodes(query.episodes(tenant(),vehicleId,driverId,from,to,cursor,limit));
    }
    @GetMapping("/episodes/{episodeId}") @PreAuthorize("hasAuthority('SPEED_EVENT_VIEW')")
    public SpeedMonitoringResponses.Episode episode(@PathVariable UUID episodeId) {
        return mapper.response(query.episode(tenant(),episodeId).orElseThrow(SpeedMonitoringController::episodeNotFound));
    }
    private UUID tenant() { return currentTenant.required().tenantId(); }
    private SpeedRuleManagementUseCase.Context context() { var c=currentTenant.required(); return new SpeedRuleManagementUseCase.Context(c.tenantId(),c.actorId(),c.correlationId(),Instant.now()); }
    private static NotFoundException ruleNotFound() { return new NotFoundException("SPEED_RULE_NOT_FOUND","Speed rule not found"); }
    private static NotFoundException stateNotFound() { return new NotFoundException("SPEED_STATE_NOT_FOUND","Speed state not found"); }
    private static NotFoundException episodeNotFound() { return new NotFoundException("SPEED_EPISODE_NOT_FOUND","Speed episode not found"); }
}
