package com.transportlogistics.app.delivery.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeliveryZoneBoundary {
    private static final double EPSILON = 1e-7;

    private final List<DeliveryZoneCoordinate> coordinates;
    private final DeliveryZoneBoundingBox boundingBox;
    private final double approximateArea;

    public DeliveryZoneBoundary(List<DeliveryZoneCoordinate> inputCoordinates) {
        if (inputCoordinates == null || inputCoordinates.size() < 4) {
            throw new IllegalArgumentException("Polygon must have at least 4 coordinates including the closing vertex");
        }
        DeliveryZoneCoordinate first = inputCoordinates.get(0);
        DeliveryZoneCoordinate last = inputCoordinates.get(inputCoordinates.size() - 1);
        if (Math.abs(first.longitude() - last.longitude()) > EPSILON || Math.abs(first.latitude() - last.latitude()) > EPSILON) {
            throw new IllegalArgumentException("Polygon ring must be closed (first coordinate must match last coordinate)");
        }

        // Distinct vertices count (excluding closing vertex)
        long distinctVertices = inputCoordinates.stream()
                .limit(inputCoordinates.size() - 1L)
                .distinct()
                .count();
        if (distinctVertices < 3) {
            throw new IllegalArgumentException("Polygon ring must have at least 3 distinct vertices");
        }

        this.coordinates = Collections.unmodifiableList(new ArrayList<>(inputCoordinates));

        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (DeliveryZoneCoordinate c : inputCoordinates) {
            if (c.latitude() < minLat) minLat = c.latitude();
            if (c.latitude() > maxLat) maxLat = c.latitude();
            if (c.longitude() < minLon) minLon = c.longitude();
            if (c.longitude() > maxLon) maxLon = c.longitude();
        }

        this.boundingBox = new DeliveryZoneBoundingBox(minLat, maxLat, minLon, maxLon);
        this.approximateArea = calculateShoelaceArea(this.coordinates);
    }

    public List<DeliveryZoneCoordinate> coordinates() {
        return coordinates;
    }

    public DeliveryZoneBoundingBox boundingBox() {
        return boundingBox;
    }

    public double approximateArea() {
        return approximateArea;
    }

    public boolean contains(double longitude, double latitude) {
        if (!boundingBox.contains(longitude, latitude)) {
            return false;
        }

        int n = coordinates.size();
        for (int i = 0; i < n - 1; i++) {
            DeliveryZoneCoordinate p1 = coordinates.get(i);
            DeliveryZoneCoordinate p2 = coordinates.get(i + 1);
            if (isPointOnSegment(longitude, latitude, p1, p2)) {
                return true; // Boundary points count as INSIDE
            }
        }

        // Ray casting algorithm
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = coordinates.get(i).longitude(), yi = coordinates.get(i).latitude();
            double xj = coordinates.get(j).longitude(), yj = coordinates.get(j).latitude();

            boolean intersect = ((yi > latitude) != (yj > latitude))
                    && (longitude < (xj - xi) * (latitude - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static boolean isPointOnSegment(double px, double py, DeliveryZoneCoordinate p1, DeliveryZoneCoordinate p2) {
        double x1 = p1.longitude(), y1 = p1.latitude();
        double x2 = p2.longitude(), y2 = p2.latitude();

        double crossProduct = (py - y1) * (x2 - x1) - (px - x1) * (y2 - y1);
        if (Math.abs(crossProduct) > EPSILON) {
            return false;
        }

        double dotProduct = (px - x1) * (x2 - x1) + (py - y1) * (y2 - y1);
        if (dotProduct < -EPSILON) {
            return false;
        }

        double squaredLength = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        if (dotProduct > squaredLength + EPSILON) {
            return false;
        }

        return true;
    }

    private static double calculateShoelaceArea(List<DeliveryZoneCoordinate> ring) {
        double area = 0.0;
        int n = ring.size();
        for (int i = 0; i < n - 1; i++) {
            DeliveryZoneCoordinate p1 = ring.get(i);
            DeliveryZoneCoordinate p2 = ring.get(i + 1);
            area += (p1.longitude() * p2.latitude() - p2.longitude() * p1.latitude());
        }
        return Math.abs(area) / 2.0;
    }
}
