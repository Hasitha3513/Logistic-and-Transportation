package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import com.transportlogistics.app.tracking.domain.TrackingModels.*;
import com.transportlogistics.app.tracking.domain.TrackingStatusPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrackingStatusPolicyTest {
 private static final Instant NOW=Instant.parse("2026-09-08T12:00:00Z");
 @Test void freshnessBoundariesAndReceiptComponentAreExact(){assertThat(fresh(59,59)).isEqualTo(Freshness.LIVE);assertThat(fresh(60,60)).isEqualTo(Freshness.LIVE);assertThat(fresh(61,61)).isEqualTo(Freshness.RECENT);assertThat(fresh(300,300)).isEqualTo(Freshness.RECENT);assertThat(fresh(301,301)).isEqualTo(Freshness.STALE);assertThat(TrackingStatusPolicy.freshness(null,NOW,NOW)).isEqualTo(Freshness.UNKNOWN);assertThat(fresh(30,61)).isEqualTo(Freshness.RECENT);}
 @Test void connectivityBoundariesAreExact(){assertThat(connect(60)).isEqualTo(Connectivity.CONNECTED);assertThat(connect(61)).isEqualTo(Connectivity.DEGRADED);assertThat(connect(300)).isEqualTo(Connectivity.DEGRADED);assertThat(connect(301)).isEqualTo(Connectivity.OFFLINE);assertThat(TrackingStatusPolicy.connectivity(null,NOW)).isEqualTo(Connectivity.UNKNOWN);}
 private static Freshness fresh(long sourceAge,long receiptAge){return TrackingStatusPolicy.freshness(observation(NOW.minusSeconds(sourceAge)),NOW.minusSeconds(receiptAge),NOW);}
 private static Connectivity connect(long age){return TrackingStatusPolicy.connectivity(NOW.minusSeconds(age),NOW);}
 private static Observation observation(Instant source){UUID id=UUID.randomUUID();return new Observation(id,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"FIXTURE","message",1L,"a".repeat(64),"b".repeat(64),source,NOW,new BigDecimal("6.9"),new BigDecimal("79.8"),BigDecimal.ONE,null,null,null,EngineState.UNKNOWN,null,null,Trust.TRUSTED,"ACCEPTABLE",Ordering.IN_ORDER,"policy","1",null,Map.of());}
}
