package com.transportlogistics.app.tracking.adapters.inbound.security;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardPage;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardQuery;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Disclosure;
import com.transportlogistics.app.tracking.ports.inbound.TrackingDashboardQueryUseCase;
import org.springframework.security.access.prepost.PreAuthorize;

public class SecuredTrackingDashboardQueryUseCase implements TrackingDashboardQueryUseCase {
    private final TrackingDashboardQueryUseCase delegate;

    public SecuredTrackingDashboardQueryUseCase(TrackingDashboardQueryUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @PreAuthorize("hasAuthority('TRACKING_DASHBOARD_VIEW')")
    public DashboardPage query(DashboardQuery query, Disclosure disclosure) {
        return delegate.query(query, disclosure);
    }
}
