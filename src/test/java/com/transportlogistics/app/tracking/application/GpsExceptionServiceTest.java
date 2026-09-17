package com.transportlogistics.app.tracking.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Severity;
import com.transportlogistics.app.tracking.ports.inbound.GpsExceptionUseCase;
import com.transportlogistics.app.tracking.ports.outbound.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GpsExceptionServiceTest {
    private final GpsExceptionRepositoryPort episodes=mock(GpsExceptionRepositoryPort.class);
    private final GpsExceptionEvidenceRepositoryPort evidence=mock(GpsExceptionEvidenceRepositoryPort.class);
    private final GpsExceptionManagementPort management=mock(GpsExceptionManagementPort.class);
    private final GpsExceptionCursorPort cursors=mock(GpsExceptionCursorPort.class);
    private final GpsExceptionTransactionPort transactions=mock(GpsExceptionTransactionPort.class);
    private GpsExceptionService service; private UUID tenant,actor,id; private Instant now; private GpsExceptionEpisode open;
    @BeforeEach void setUp(){tenant=UUID.randomUUID();actor=UUID.randomUUID();id=UUID.randomUUID();now=Instant.parse("2026-09-17T10:00:00Z");
        open=GpsExceptionEpisode.open(id,tenant,UUID.randomUUID(),UUID.randomUUID(),ExceptionType.PROCESSING_FAILURE,Severity.WARNING,now.minusSeconds(60));
        when(transactions.execute(any())).thenAnswer(i->((java.util.function.Supplier<?>)i.getArgument(0)).get());
        service=new GpsExceptionService(episodes,evidence,management,cursors,transactions);}
    @Test void acknowledgesOnceAndReturnsStoredSnapshotOnRetry(){AtomicReference<GpsExceptionManagementPort.Command> command=new AtomicReference<>();
        when(management.command(eq(tenant),anyString())).thenAnswer(i->Optional.ofNullable(command.get()));when(episodes.findByIdForUpdate(tenant,id)).thenReturn(Optional.of(open));
        when(episodes.save(any())).thenAnswer(i->i.getArgument(0));doAnswer(i->{command.set(new GpsExceptionManagementPort.Command(i.getArgument(2),actor,i.getArgument(6)));return null;}).when(management).complete(eq(tenant),anyString(),anyString(),eq(actor),eq(id),eq(0L),any(),eq(now));
        var context=new GpsExceptionUseCase.Context(tenant,actor,"corr",now);String key="0123456789abcdef";
        var first=service.acknowledge(context,id,0,"  reviewed safely  ",key);var replay=service.acknowledge(context,id,0,"reviewed safely",key);
        assertThat(replay).isEqualTo(first);verify(episodes,times(1)).save(any());verify(management,times(1)).audit(tenant,actor,id,"corr",now);}
    @Test void conflictingKeyDoesNotDiscloseStoredResponse(){var response=new GpsExceptionUseCase.Acknowledgement(id,com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EpisodeStatus.ACKNOWLEDGED,Severity.WARNING,1,now);
        when(management.command(eq(tenant),anyString())).thenReturn(Optional.of(new GpsExceptionManagementPort.Command("different",actor,response)));
        assertThatThrownBy(()->service.acknowledge(new GpsExceptionUseCase.Context(tenant,actor,"c",now),id,0,"reviewed","0123456789abcdef"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("another request");verifyNoInteractions(episodes);}
    @Test void validatesSevenDayRangeAndReasonControls(){var c=new GpsExceptionUseCase.Context(tenant,actor,"c",now);
        assertThatThrownBy(()->service.episodes(c,new GpsExceptionUseCase.Filter(now.minusSeconds(8*86400),now,null,null,null,null,null),null,100)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.acknowledge(c,id,0,"bad\nreason","0123456789abcdef")).isInstanceOf(IllegalArgumentException.class);}
}
