package com.transportlogistics.app.tracking.domain.routedeviation;

public final class RouteDistanceCalculator {
    private RouteDistanceCalculator() { }

    public static DistanceMeters minimumDistance(RoutePoint observation, RoutePolyline route) {
        double originLat = Math.toRadians(observation.latitude().doubleValue());
        double scale = Math.cos(originLat);
        double best = Double.POSITIVE_INFINITY;
        for (int i=1;i<route.points().size();i++) {
            RoutePoint a=route.points().get(i-1), b=route.points().get(i);
            double ax=Math.toRadians(a.longitude().doubleValue()-observation.longitude().doubleValue())*scale*RoutePolyline.EARTH_RADIUS_METERS;
            double ay=Math.toRadians(a.latitude().doubleValue()-observation.latitude().doubleValue())*RoutePolyline.EARTH_RADIUS_METERS;
            double bx=Math.toRadians(b.longitude().doubleValue()-observation.longitude().doubleValue())*scale*RoutePolyline.EARTH_RADIUS_METERS;
            double by=Math.toRadians(b.latitude().doubleValue()-observation.latitude().doubleValue())*RoutePolyline.EARTH_RADIUS_METERS;
            double dx=bx-ax,dy=by-ay, length=dx*dx+dy*dy;
            double t=length==0?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/length));
            best=Math.min(best,Math.hypot(ax+t*dx,ay+t*dy));
        }
        return DistanceMeters.of(best);
    }
}
