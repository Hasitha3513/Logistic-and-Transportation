package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardPage;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardQuery;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Disclosure;

public interface TrackingDashboardQueryUseCase {
    DashboardPage query(DashboardQuery query, Disclosure disclosure);
}
