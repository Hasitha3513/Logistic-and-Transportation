package com.transportlogistics.app.tracking.domain.geofence;

import java.util.ArrayList;
import java.util.List;

public final class GeofencePolygon {
    private static final double EPSILON = 1.0e-12;
    private final List<Wgs84Coordinate> vertices;
    private final BoundingBox boundingBox;

    private GeofencePolygon(List<Wgs84Coordinate> vertices) {
        this.vertices = List.copyOf(vertices);
        this.boundingBox = bounds(vertices);
    }

    public static GeofencePolygon of(List<Wgs84Coordinate> openRing) {
        if (openRing == null) {
            throw invalid("Polygon vertices are required");
        }
        List<Wgs84Coordinate> vertices = new ArrayList<>(openRing);
        if (vertices.size() < 3 || vertices.size() > 100) {
            throw invalid("Polygon requires between 3 and 100 vertices");
        }
        if (vertices.stream().distinct().count() != vertices.size()) {
            throw invalid("Polygon vertices must be distinct");
        }
        for (int index = 0; index < vertices.size(); index++) {
            if (vertices.get(index) == null) {
                throw invalid("Polygon vertices cannot be null");
            }
            if (vertices.get(index).equals(vertices.get((index + 1) % vertices.size()))) {
                throw invalid("Consecutive polygon vertices cannot be equal");
            }
        }
        if (Math.abs(signedArea(vertices)) <= EPSILON) {
            throw invalid("Polygon area must be greater than zero");
        }
        rejectIntersections(vertices);
        return new GeofencePolygon(vertices);
    }

    public List<Wgs84Coordinate> vertices() {
        return vertices;
    }

    public List<Wgs84Coordinate> closedRing() {
        List<Wgs84Coordinate> ring = new ArrayList<>(vertices);
        ring.add(vertices.getFirst());
        return List.copyOf(ring);
    }

    public BoundingBox boundingBox() {
        return boundingBox;
    }

    public boolean contains(Wgs84Coordinate point) {
        if (point == null || !boundingBox.contains(point)) {
            return false;
        }
        boolean inside = false;
        for (int current = 0, previous = vertices.size() - 1;
             current < vertices.size(); previous = current++) {
            Wgs84Coordinate a = vertices.get(previous);
            Wgs84Coordinate b = vertices.get(current);
            if (onSegment(a, point, b)) {
                return true;
            }
            boolean crosses = (a.latitude() > point.latitude()) != (b.latitude() > point.latitude());
            if (crosses) {
                double longitude = (b.longitude() - a.longitude())
                        * (point.latitude() - a.latitude()) / (b.latitude() - a.latitude())
                        + a.longitude();
                if (point.longitude() < longitude) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private static BoundingBox bounds(List<Wgs84Coordinate> vertices) {
        double minLongitude = vertices.stream().mapToDouble(Wgs84Coordinate::longitude).min().orElseThrow();
        double maxLongitude = vertices.stream().mapToDouble(Wgs84Coordinate::longitude).max().orElseThrow();
        double minLatitude = vertices.stream().mapToDouble(Wgs84Coordinate::latitude).min().orElseThrow();
        double maxLatitude = vertices.stream().mapToDouble(Wgs84Coordinate::latitude).max().orElseThrow();
        return new BoundingBox(minLongitude, minLatitude, maxLongitude, maxLatitude);
    }

    private static double signedArea(List<Wgs84Coordinate> vertices) {
        double sum = 0.0;
        for (int index = 0; index < vertices.size(); index++) {
            Wgs84Coordinate current = vertices.get(index);
            Wgs84Coordinate next = vertices.get((index + 1) % vertices.size());
            sum += current.longitude() * next.latitude() - next.longitude() * current.latitude();
        }
        return sum / 2.0;
    }

    private static void rejectIntersections(List<Wgs84Coordinate> vertices) {
        int size = vertices.size();
        for (int first = 0; first < size; first++) {
            Wgs84Coordinate a = vertices.get(first);
            Wgs84Coordinate b = vertices.get((first + 1) % size);
            for (int second = first + 1; second < size; second++) {
                if (first == second || (first + 1) % size == second || (second + 1) % size == first) {
                    continue;
                }
                Wgs84Coordinate c = vertices.get(second);
                Wgs84Coordinate d = vertices.get((second + 1) % size);
                if (intersects(a, b, c, d)) {
                    throw invalid("Polygon must not self-intersect");
                }
            }
        }
    }

    private static boolean intersects(Wgs84Coordinate a, Wgs84Coordinate b,
                                      Wgs84Coordinate c, Wgs84Coordinate d) {
        double first = orientation(a, b, c);
        double second = orientation(a, b, d);
        double third = orientation(c, d, a);
        double fourth = orientation(c, d, b);
        if (((first > EPSILON && second < -EPSILON) || (first < -EPSILON && second > EPSILON))
                && ((third > EPSILON && fourth < -EPSILON)
                || (third < -EPSILON && fourth > EPSILON))) {
            return true;
        }
        return Math.abs(first) <= EPSILON && onSegment(a, c, b)
                || Math.abs(second) <= EPSILON && onSegment(a, d, b)
                || Math.abs(third) <= EPSILON && onSegment(c, a, d)
                || Math.abs(fourth) <= EPSILON && onSegment(c, b, d);
    }

    private static double orientation(Wgs84Coordinate a, Wgs84Coordinate b, Wgs84Coordinate c) {
        return (b.longitude() - a.longitude()) * (c.latitude() - a.latitude())
                - (b.latitude() - a.latitude()) * (c.longitude() - a.longitude());
    }

    private static boolean onSegment(Wgs84Coordinate a, Wgs84Coordinate point, Wgs84Coordinate b) {
        return Math.abs(orientation(a, b, point)) <= EPSILON
                && point.longitude() >= Math.min(a.longitude(), b.longitude()) - EPSILON
                && point.longitude() <= Math.max(a.longitude(), b.longitude()) + EPSILON
                && point.latitude() >= Math.min(a.latitude(), b.latitude()) - EPSILON
                && point.latitude() <= Math.max(a.latitude(), b.latitude()) + EPSILON;
    }

    private static GeofenceRuleException invalid(String message) {
        return new GeofenceRuleException("GEOFENCE_INVALID_GEOMETRY", message);
    }
}
