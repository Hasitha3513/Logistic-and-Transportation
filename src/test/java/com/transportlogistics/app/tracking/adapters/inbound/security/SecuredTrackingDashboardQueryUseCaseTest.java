package com.transportlogistics.app.tracking.adapters.inbound.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardQuery;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Disclosure;
import com.transportlogistics.app.tracking.ports.inbound.TrackingDashboardQueryUseCase;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class SecuredTrackingDashboardQueryUseCaseTest {
    @Test
    void useCaseBoundaryRequiresTheNarrowDashboardPermission() throws Exception {
        Method query = SecuredTrackingDashboardQueryUseCase.class.getMethod(
                "query", DashboardQuery.class, Disclosure.class);
        assertThat(query.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('TRACKING_DASHBOARD_VIEW')");
        assertThat(TrackingDashboardQueryUseCase.class
                .isAssignableFrom(SecuredTrackingDashboardQueryUseCase.class)).isTrue();
    }
}
