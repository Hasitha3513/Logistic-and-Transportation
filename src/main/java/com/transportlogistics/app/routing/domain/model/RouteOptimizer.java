package com.transportlogistics.app.routing.domain.model;

import java.util.*;
import java.util.function.BiFunction;

public final class RouteOptimizer {

    public static final int MAX_OPTIMIZATION_STOPS = 50;

    private RouteOptimizer() {
    }

    public static RouteOptimizationResult optimize(
            UUID routeId,
            UUID originLocationId,
            UUID destinationLocationId,
            List<UUID> stopLocationIds,
            double originalPlannedDistanceKm,
            int originalEstimatedDurationMinutes,
            BiFunction<UUID, UUID, Double> distanceFunction
    ) {
        Objects.requireNonNull(routeId, "routeId is required");
        Objects.requireNonNull(originLocationId, "originLocationId is required");
        Objects.requireNonNull(destinationLocationId, "destinationLocationId is required");
        Objects.requireNonNull(distanceFunction, "distanceFunction is required");

        List<UUID> originalStops = stopLocationIds == null ? List.of() : List.copyOf(stopLocationIds);

        if (originalStops.stream().anyMatch(Objects::isNull)
                || new HashSet<>(originalStops).size() != originalStops.size()
                || originalStops.contains(originLocationId)
                || originalStops.contains(destinationLocationId)) {
            throw new IllegalArgumentException("Route optimization stops must be unique, non-null intermediate locations");
        }

        if (originalStops.size() > MAX_OPTIMIZATION_STOPS) {
            throw new IllegalArgumentException("Cannot optimize routes with more than " + MAX_OPTIMIZATION_STOPS + " stops");
        }

        if (originalStops.size() <= 1) {
            return new RouteOptimizationResult(
                    routeId,
                    originalStops,
                    originalStops,
                    originalPlannedDistanceKm,
                    originalPlannedDistanceKm,
                    originalEstimatedDurationMinutes,
                    originalEstimatedDurationMinutes,
                    0.0,
                    0,
                    0.0
            );
        }

        // Calculate original total pairwise distance
        double originalTourDistance = calculateTourDistance(originLocationId, destinationLocationId, originalStops, distanceFunction);

        // Step 1: Nearest Neighbor heuristic from Origin -> intermediate stops -> Destination
        List<UUID> nnStops = solveNearestNeighbor(originLocationId, destinationLocationId, originalStops, distanceFunction);

        // Step 2: 2-Opt local search improvement on intermediate stops
        List<UUID> optimizedStops = solveTwoOpt(originLocationId, destinationLocationId, nnStops, distanceFunction);

        double optimizedTourDistance = calculateTourDistance(originLocationId, destinationLocationId, optimizedStops, distanceFunction);

        // Guarantee: If optimization produces a shorter route, use it; otherwise retain original
        if (optimizedTourDistance < originalTourDistance - 1e-4) {
            double distanceRatio = originalTourDistance > 0 ? (optimizedTourDistance / originalTourDistance) : 1.0;
            double optimizedDistanceKm = originalPlannedDistanceKm > 0
                    ? originalPlannedDistanceKm * distanceRatio
                    : optimizedTourDistance;
            int optimizedDurationMinutes = originalEstimatedDurationMinutes > 0
                    ? Math.max(1, (int) Math.round(originalEstimatedDurationMinutes * distanceRatio))
                    : originalEstimatedDurationMinutes;

            double distanceSavedKm = Math.max(0.0, originalPlannedDistanceKm - optimizedDistanceKm);
            int durationSavedMinutes = Math.max(0, originalEstimatedDurationMinutes - optimizedDurationMinutes);
            double percentageImprovement = originalPlannedDistanceKm > 0
                    ? (distanceSavedKm / originalPlannedDistanceKm) * 100.0
                    : 0.0;

            return new RouteOptimizationResult(
                    routeId,
                    originalStops,
                    optimizedStops,
                    originalPlannedDistanceKm,
                    optimizedDistanceKm,
                    originalEstimatedDurationMinutes,
                    optimizedDurationMinutes,
                    distanceSavedKm,
                    durationSavedMinutes,
                    percentageImprovement
            );
        }

        return new RouteOptimizationResult(
                routeId,
                originalStops,
                originalStops,
                originalPlannedDistanceKm,
                originalPlannedDistanceKm,
                originalEstimatedDurationMinutes,
                originalEstimatedDurationMinutes,
                0.0,
                0,
                0.0
        );
    }

    private static List<UUID> solveNearestNeighbor(
            UUID origin,
            UUID destination,
            List<UUID> stops,
            BiFunction<UUID, UUID, Double> dist
    ) {
        List<UUID> unvisited = new ArrayList<>(stops);
        List<UUID> result = new ArrayList<>(stops.size());
        UUID current = origin;

        while (!unvisited.isEmpty()) {
            UUID bestNext = null;
            double minDistance = Double.MAX_VALUE;

            for (UUID candidate : unvisited) {
                double d = checkedDistance(current, candidate, dist);
                if (d < minDistance) {
                    minDistance = d;
                    bestNext = candidate;
                }
            }

            result.add(bestNext);
            unvisited.remove(bestNext);
            current = bestNext;
        }

        return result;
    }

    private static List<UUID> solveTwoOpt(
            UUID origin,
            UUID destination,
            List<UUID> initialStops,
            BiFunction<UUID, UUID, Double> dist
    ) {
        int n = initialStops.size();
        // Full tour array: index 0 = origin, 1..n = stops, n+1 = destination
        UUID[] tour = new UUID[n + 2];
        tour[0] = origin;
        for (int i = 0; i < n; i++) {
            tour[i + 1] = initialStops.get(i);
        }
        tour[n + 1] = destination;

        boolean improved = true;
        int maxIterations = 500;
        int iteration = 0;

        while (improved && iteration++ < maxIterations) {
            improved = false;
            for (int i = 1; i <= n; i++) {
                for (int j = i + 1; j <= n; j++) {
                    double currentDist = checkedDistance(tour[i - 1], tour[i], dist)
                            + checkedDistance(tour[j], tour[j + 1], dist);
                    double swappedDist = checkedDistance(tour[i - 1], tour[j], dist)
                            + checkedDistance(tour[i], tour[j + 1], dist);

                    if (swappedDist < currentDist - 1e-6) {
                        // Reverse segment tour[i..j]
                        reverseSegment(tour, i, j);
                        improved = true;
                    }
                }
            }
        }

        List<UUID> result = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            result.add(tour[i]);
        }
        return Collections.unmodifiableList(result);
    }

    private static void reverseSegment(UUID[] tour, int i, int j) {
        while (i < j) {
            UUID temp = tour[i];
            tour[i] = tour[j];
            tour[j] = temp;
            i++;
            j--;
        }
    }

    private static double calculateTourDistance(
            UUID origin,
            UUID destination,
            List<UUID> stops,
            BiFunction<UUID, UUID, Double> dist
    ) {
        if (stops.isEmpty()) {
            return checkedDistance(origin, destination, dist);
        }

        double total = checkedDistance(origin, stops.get(0), dist);
        for (int i = 0; i < stops.size() - 1; i++) {
            total += checkedDistance(stops.get(i), stops.get(i + 1), dist);
        }
        total += checkedDistance(stops.get(stops.size() - 1), destination, dist);
        return total;
    }

    private static double checkedDistance(UUID from, UUID to, BiFunction<UUID, UUID, Double> dist) {
        Double value = dist.apply(from, to);
        if (value == null || !Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("Route optimization distance data must be finite and non-negative");
        }
        return value;
    }
}
