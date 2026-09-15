package com.transportlogistics.app.tracking.adapters.inbound.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopReplayQuery;
import com.transportlogistics.app.tracking.ports.inbound.JourneyReplayQueryUseCase;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class SecuredJourneyReplayQueryUseCaseTest {
    @Test
    void useCaseBoundaryDeclaresIndependentReplayAndIncidentPermissions() throws Exception {
        Method points = SecuredJourneyReplayQueryUseCase.class.getMethod("points", ReplayQuery.class);
        Method stops = SecuredJourneyReplayQueryUseCase.class.getMethod("stops", StopReplayQuery.class);
        Method incidents = SecuredJourneyReplayQueryUseCase.class.getMethod("incidents", ReplayQuery.class);
        assertThat(points.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasAuthority('JOURNEY_REPLAY_VIEW')");
        assertThat(stops.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasAuthority('JOURNEY_REPLAY_VIEW')");
        assertThat(incidents.getAnnotation(PreAuthorize.class).value())
                .contains("JOURNEY_REPLAY_VIEW", "JOURNEY_REPLAY_INCIDENT_VIEW", " and ");
        assertThat(JourneyReplayQueryUseCase.class.isAssignableFrom(SecuredJourneyReplayQueryUseCase.class)).isTrue();
    }
}
