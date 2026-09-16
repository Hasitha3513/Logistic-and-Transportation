package com.transportlogistics.app.tracking.domain.dashboard;

import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Motion.MOVING;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Motion.STATIONARY;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Motion.UNKNOWN;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardQuery;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Freshness;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.HeatMapCell;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Motion;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Observation;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Trust;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class TrackingDashboardPolicy {
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAXIMUM_PAGE_SIZE = 100;
    public static final int MAXIMUM_VEHICLE_FILTER = 100;
    public static final int MAXIMUM_INCIDENTS = 50;
    public static final int MAXIMUM_INCIDENTS_PER_PRODUCER = 20;
    public static final int MAXIMUM_HEAT_MAP_CELLS = 100;
    public static final BigDecimal STATIONARY_SPEED_KPH = new BigDecimal("3.0");
    public static final BigDecimal HEAT_CELL_DEGREES = new BigDecimal("0.01");

    private TrackingDashboardPolicy() {
    }

    public static DashboardQuery validate(DashboardQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        if (query.tenantId() == null) {
            throw invalid("Tenant is required");
        }
        if (query.filter() == null) {
            throw invalid("Dashboard filter is required");
        }
        if (query.filter().vehicleIds().size() > MAXIMUM_VEHICLE_FILTER) {
            throw invalid("At most 100 Vehicle identifiers are permitted");
        }
        if (query.pageSize() < 1 || query.pageSize() > MAXIMUM_PAGE_SIZE) {
            throw invalid("Page size must be between 1 and 100");
        }
        return query;
    }

    public static Motion motion(Observation latestTrusted) {
        if (latestTrusted == null || latestTrusted.trust() != Trust.TRUSTED
                || latestTrusted.speedKph() == null) {
            return UNKNOWN;
        }
        return latestTrusted.speedKph().compareTo(STATIONARY_SPEED_KPH) > 0 ? MOVING : STATIONARY;
    }

    public static boolean heatEligible(Freshness freshness, Observation latestTrusted) {
        return (freshness == Freshness.LIVE || freshness == Freshness.RECENT)
                && latestTrusted != null
                && latestTrusted.trust() == Trust.TRUSTED
                && latestTrusted.latitude() != null
                && latestTrusted.longitude() != null;
    }

    public static HeatMapCell heatCell(Observation observation, long count) {
        Objects.requireNonNull(observation, "observation must not be null");
        if (observation.latitude() == null || observation.longitude() == null || count < 1) {
            throw invalid("Heat-map cell requires coordinates and a positive count");
        }
        return new HeatMapCell(center(observation.latitude()), center(observation.longitude()), count);
    }

    private static BigDecimal center(BigDecimal coordinate) {
        BigDecimal lower = coordinate.divide(HEAT_CELL_DEGREES, 0, RoundingMode.FLOOR)
                .multiply(HEAT_CELL_DEGREES);
        return lower.add(HEAT_CELL_DEGREES.divide(BigDecimal.valueOf(2))).setScale(3, RoundingMode.UNNECESSARY);
    }

    private static TrackingDashboardException invalid(String message) {
        return new TrackingDashboardException("TRACKING_DASHBOARD_QUERY_INVALID", message);
    }
}
