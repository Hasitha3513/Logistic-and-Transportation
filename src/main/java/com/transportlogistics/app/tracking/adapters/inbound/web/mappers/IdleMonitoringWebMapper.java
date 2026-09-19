package com.transportlogistics.app.tracking.adapters.inbound.web.mappers;

import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.IdleMonitoringResponses;
import com.transportlogistics.app.tracking.ports.inbound.IdleMonitoringQuery;
import org.springframework.stereotype.Component;

@Component
public class IdleMonitoringWebMapper {
    public IdleMonitoringResponses.State state(IdleMonitoringQuery.State value){return new IdleMonitoringResponses.State(value.vehicleId(),null,value.state(),value.capabilityState(),value.latestSourceTimestamp(),value.candidateStartedAt(),value.lastQualifyingAt(),value.creditedSeconds(),value.evidenceCount(),value.version(),"UNAVAILABLE",null);}
    public IdleMonitoringResponses.Episode episode(IdleMonitoringQuery.Episode value){return new IdleMonitoringResponses.Episode(value.id(),value.vehicleId(),null,value.lifecycle(),value.startSourceTimestamp(),value.confirmedAt(),value.lastSourceTimestamp(),value.endSourceTimestamp(),value.endReason(),value.creditedSeconds(),value.evidenceCount(),value.version(),"UNAVAILABLE",null);}
    public IdleMonitoringResponses.Evidence evidence(IdleMonitoringQuery.Evidence value){return new IdleMonitoringResponses.Evidence(value.id(),value.sourceTimestamp(),value.outcome(),value.creditedDeltaSeconds(),value.recordedAt());}
    public IdleMonitoringResponses.StatePage states(IdleMonitoringQuery.Page<IdleMonitoringQuery.State> page){return new IdleMonitoringResponses.StatePage(page.items().stream().map(this::state).toList(),page.nextCursor());}
    public IdleMonitoringResponses.EpisodePage episodes(IdleMonitoringQuery.Page<IdleMonitoringQuery.Episode> page){return new IdleMonitoringResponses.EpisodePage(page.items().stream().map(this::episode).toList(),page.nextCursor());}
    public IdleMonitoringResponses.EvidencePage evidence(IdleMonitoringQuery.Page<IdleMonitoringQuery.Evidence> page){return new IdleMonitoringResponses.EvidencePage(page.items().stream().map(this::evidence).toList(),page.nextCursor());}
}
