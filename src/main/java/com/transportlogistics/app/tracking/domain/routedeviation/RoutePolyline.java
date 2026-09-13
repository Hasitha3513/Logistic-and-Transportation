package com.transportlogistics.app.tracking.domain.routedeviation;

import java.util.List;
import java.util.Objects;

public record RoutePolyline(RouteVersion routeVersion, List<RoutePoint> points) {
    static final double EARTH_RADIUS_METERS = 6_371_000d;
    static final double MAX_SEGMENT_METERS = 25_000d;

    public RoutePolyline {
        Objects.requireNonNull(routeVersion, "Route version is required");
        points = List.copyOf(Objects.requireNonNull(points, "Points are required"));
        if (points.size() < 2 || points.size() > 2_000) throw error("INVALID_GEOMETRY", "Polyline requires 2 to 2000 points");
        for (int i = 1; i < points.size(); i++) {
            RoutePoint a = points.get(i - 1); RoutePoint b = points.get(i);
            if (a.equals(b)) throw error("INVALID_GEOMETRY", "Adjacent points must differ");
            if (Math.abs(a.longitude().doubleValue() - b.longitude().doubleValue()) > 180d) {
                throw error("UNSUPPORTED_ANTIMERIDIAN", "Antimeridian routes are unsupported");
            }
            if (greatCircleMeters(a, b) > MAX_SEGMENT_METERS + 0.001d) {
                throw error("SEGMENT_TOO_LONG", "Route segments cannot exceed 25 km");
            }
        }
    }

    static double greatCircleMeters(RoutePoint a, RoutePoint b) {
        double lat1=Math.toRadians(a.latitude().doubleValue()), lat2=Math.toRadians(b.latitude().doubleValue());
        double dlat=lat2-lat1, dlon=Math.toRadians(b.longitude().doubleValue()-a.longitude().doubleValue());
        double h=Math.sin(dlat/2)*Math.sin(dlat/2)+Math.cos(lat1)*Math.cos(lat2)*Math.sin(dlon/2)*Math.sin(dlon/2);
        return 2*EARTH_RADIUS_METERS*Math.atan2(Math.sqrt(h),Math.sqrt(1-h));
    }
    private static RouteDeviationException error(String code,String message){return new RouteDeviationException(code,message);}
}
