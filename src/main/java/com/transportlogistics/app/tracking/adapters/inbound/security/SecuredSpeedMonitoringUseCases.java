package com.transportlogistics.app.tracking.adapters.inbound.security;

import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import com.transportlogistics.app.tracking.domain.speed.VehicleSpeedState;
import com.transportlogistics.app.tracking.ports.inbound.SpeedMonitoringQuery;
import com.transportlogistics.app.tracking.ports.inbound.SpeedRuleManagementUseCase;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

public class SecuredSpeedMonitoringUseCases implements SpeedRuleManagementUseCase, SpeedMonitoringQuery {
    private final SpeedRuleManagementUseCase management;
    private final SpeedMonitoringQuery query;
    public SecuredSpeedMonitoringUseCases(SpeedRuleManagementUseCase management,
                                          SpeedMonitoringQuery query) {
        this.management = management; this.query = query;
    }
    @Override @PreAuthorize("hasAuthority('SPEED_MONITOR_MANAGE')")
    public SpeedRule create(Context c, CreateRule x, String k) { return management.create(c, x, k); }
    @Override @PreAuthorize("hasAuthority('SPEED_MONITOR_MANAGE')")
    public SpeedRule update(Context c, UUID id, long v, UpdateRule x) { return management.update(c,id,v,x); }
    @Override @PreAuthorize("hasAuthority('SPEED_MONITOR_MANAGE')")
    public SpeedRule activate(Context c, UUID id, long v, String k) { return management.activate(c,id,v,k); }
    @Override @PreAuthorize("hasAuthority('SPEED_MONITOR_MANAGE')")
    public SpeedRule disable(Context c, UUID id, long v, String r, String k) { return management.disable(c,id,v,r,k); }
    @Override @PreAuthorize("hasAuthority('SPEED_MONITOR_MANAGE')")
    public SpeedRule retire(Context c, UUID id, long v, String r, String k) { return management.retire(c,id,v,r,k); }
    @Override @PreAuthorize("hasAuthority('SPEED_MONITOR_VIEW')")
    public Optional<SpeedRule> rule(UUID t, UUID id) { return query.rule(t,id); }
    @Override @PreAuthorize("hasAuthority('SPEED_MONITOR_VIEW')")
    public Page<SpeedRule> rules(UUID t, SpeedRule.Scope s, SpeedRule.Lifecycle l, int p, int z) { return query.rules(t,s,l,p,z); }
    @Override @PreAuthorize("hasAuthority('SPEED_MONITOR_VIEW')")
    public Optional<VehicleSpeedState> state(UUID t, UUID v) { return query.state(t,v); }
    @Override @PreAuthorize("hasAuthority('SPEED_MONITOR_VIEW')")
    public Page<VehicleSpeedState> states(UUID t, VehicleSpeedState.MonitoringState s, int p, int z) { return query.states(t,s,p,z); }
    @Override @PreAuthorize("hasAuthority('SPEED_EVENT_VIEW')")
    public Optional<SpeedingEpisode> episode(UUID t, UUID id) { return query.episode(t,id); }
    @Override @PreAuthorize("hasAuthority('SPEED_EVENT_VIEW')")
    public CursorPage<SpeedingEpisode> episodes(UUID t, UUID v, UUID d, Instant f, Instant o, String c, int l) { return query.episodes(t,v,d,f,o,c,l); }
}
