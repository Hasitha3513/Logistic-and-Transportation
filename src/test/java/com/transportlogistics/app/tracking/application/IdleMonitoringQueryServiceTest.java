package com.transportlogistics.app.tracking.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.transportlogistics.app.tracking.ports.inbound.IdleMonitoringQuery;
import com.transportlogistics.app.tracking.ports.outbound.IdleMonitoringAuditPort;
import com.transportlogistics.app.tracking.ports.outbound.IdleMonitoringCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.IdleMonitoringReadPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdleMonitoringQueryServiceTest {
    private final IdleMonitoringReadPort reads=mock(IdleMonitoringReadPort.class);
    private final IdleMonitoringCursorPort cursors=mock(IdleMonitoringCursorPort.class);
    private final IdleMonitoringAuditPort audit=mock(IdleMonitoringAuditPort.class);
    private final IdleMonitoringQueryService service=new IdleMonitoringQueryService(reads,cursors,audit);
    private final UUID tenant=UUID.randomUUID(),actor=UUID.randomUUID(),vehicle=UUID.randomUUID();
    private final Instant now=Instant.parse("2026-09-19T12:00:00Z");
    private IdleMonitoringQuery.Context context(){return new IdleMonitoringQuery.Context(tenant,actor,"corr",now);}

    @Test void validatesBoundsRangeAndVocabulary(){
        assertThatThrownBy(()->service.states(context(),new IdleMonitoringQuery.StateFilter(null,null),null,101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.states(context(),new IdleMonitoringQuery.StateFilter(null,"BROKEN"),null,50)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.episodes(context(),new IdleMonitoringQuery.EpisodeFilter(null,now.minusSeconds(32L*86400),now,null),null,50)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.episodes(context(),new IdleMonitoringQuery.EpisodeFilter(null,now.minusSeconds(60),now,"BAD"),null,50)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void stateCursorIsTenantAndFilterBoundAndAudited(){
        var one=new IdleMonitoringQuery.State(vehicle,"IDLE","SUPPORTED",now,null,now,300,2,1);
        var two=new IdleMonitoringQuery.State(UUID.randomUUID(),"NORMAL","SUPPORTED",now.minusSeconds(1),null,null,0,1,0);
        when(reads.states(eq(tenant),any(),isNull(),isNull(),eq(2))).thenReturn(List.of(one,two));
        when(cursors.encode(eq(tenant),contains("IDLE"),eq(now),eq(vehicle))).thenReturn("next");
        var result=service.states(context(),new IdleMonitoringQuery.StateFilter(null,"IDLE"),null,1);
        assertThat(result.items()).containsExactly(one);assertThat(result.nextCursor()).isEqualTo("next");
        verify(audit).record(eq(tenant),eq(actor),eq("corr"),eq("IDLE_MONITOR_STATES_VIEWED"),isNull(),eq("vehicle=false;state=true"),eq(1),eq(1),eq(now));
    }

    @Test void foreignOrCandidateEpisodeIsReportedAsSafeAbsenceByReadBoundary(){
        UUID episode=UUID.randomUUID();when(reads.episode(tenant,episode)).thenReturn(java.util.Optional.empty());
        assertThat(service.episode(context(),episode)).isEmpty();
        assertThatThrownBy(()->service.evidence(context(),episode,null,50)).hasMessage("Idle episode not found");
        verify(reads,never()).evidence(any(),any(),any(),any(),anyInt());
    }
}
