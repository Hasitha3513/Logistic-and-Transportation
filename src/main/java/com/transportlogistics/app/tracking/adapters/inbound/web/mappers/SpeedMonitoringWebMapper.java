package com.transportlogistics.app.tracking.adapters.inbound.web.mappers;

import com.transportlogistics.app.tracking.adapters.inbound.web.dto.request.SpeedMonitoringRequests;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.SpeedMonitoringResponses;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import com.transportlogistics.app.tracking.domain.speed.VehicleSpeedState;
import com.transportlogistics.app.tracking.ports.inbound.SpeedMonitoringQuery;
import com.transportlogistics.app.tracking.ports.inbound.SpeedRuleManagementUseCase;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface SpeedMonitoringWebMapper {
    default SpeedRuleManagementUseCase.CreateRule command(SpeedMonitoringRequests.Create r) {
        return new SpeedRuleManagementUseCase.CreateRule(r.name(),r.scope(),r.routeId(),r.routeVersion(),SpeedKph.threshold(r.thresholdKph()));
    }
    default SpeedRuleManagementUseCase.UpdateRule command(SpeedMonitoringRequests.Update r) {
        return new SpeedRuleManagementUseCase.UpdateRule(r.name(),r.routeId(),r.routeVersion(),SpeedKph.threshold(r.thresholdKph()));
    }
    default SpeedMonitoringResponses.Rule response(SpeedRule v) { return new SpeedMonitoringResponses.Rule(v.id(),v.name(),v.scope().name(),v.routeId(),v.routeVersion(),v.thresholdKph().value(),v.lifecycle().name(),v.ruleVersion(),v.effectiveAt()); }
    default SpeedMonitoringResponses.State response(VehicleSpeedState v) { return new SpeedMonitoringResponses.State(v.vehicleId(),v.monitoringState().name(),v.availability().name(),v.effectiveRuleId(),v.effectiveRuleId()==null?null:v.effectiveRuleVersion(),v.activeEpisodeId(),v.lastEvaluatedSourceTimestamp()); }
    default SpeedMonitoringResponses.Episode response(SpeedingEpisode v) { return new SpeedMonitoringResponses.Episode(v.id(),v.vehicleId(),v.attribution().tripId(),v.attribution().driverId(),v.attribution().routeId(),v.attribution().routeVersion(),v.ruleId(),v.ruleVersion(),v.thresholdSource().name(),v.effectiveThresholdKph().value(),v.startSourceTimestamp(),v.confirmationSourceTimestamp(),v.endSourceTimestamp(),v.maxObservedSpeedKph().value(),v.eligibleAboveThresholdSampleCount(),v.severity().name(),v.repeatCount()); }
    default SpeedMonitoringResponses.RulePage rules(SpeedMonitoringQuery.Page<SpeedRule> p) { return new SpeedMonitoringResponses.RulePage(p.items().stream().map(this::response).toList(),p.page(),p.size(),p.total()); }
    default SpeedMonitoringResponses.StatePage states(SpeedMonitoringQuery.Page<VehicleSpeedState> p) { return new SpeedMonitoringResponses.StatePage(p.items().stream().map(this::response).toList(),p.page(),p.size(),p.total()); }
    default SpeedMonitoringResponses.EpisodePage episodes(SpeedMonitoringQuery.CursorPage<SpeedingEpisode> p) { return new SpeedMonitoringResponses.EpisodePage(p.items().stream().map(this::response).toList(),p.nextCursor()); }
}
