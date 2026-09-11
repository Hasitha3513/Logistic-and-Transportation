package com.transportlogistics.app.tracking.adapters.inbound.web.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class GeofenceResponses {
    private GeofenceResponses() {
    }

    public record Coordinate(double longitude, double latitude) {
    }

    public record AlertPolicy(boolean alertOnEntry, boolean alertOnExit) {
    }

    public record Definition(
            UUID id, String name, String type, List<Coordinate> polygon, UUID locationId,
            AlertPolicy alertPolicy, String lifecycle, long version,
            Instant createdAt, Instant updatedAt) {
    }

    public record DefinitionPage(List<Definition> items, int page, int size, long total) {
    }

    public record Membership(UUID geofenceId, UUID vehicleId, String state,
                             long definitionVersion, Instant sourceTimestamp) {
    }

    public record MembershipPage(List<Membership> items, int page, int size, long total) {
    }

    public record Transition(
            UUID id, UUID geofenceId, UUID vehicleId, UUID locationId, String geofenceType,
            String transition, String severity, Instant sourceTimestamp, long definitionVersion) {
    }

    public record TransitionPage(List<Transition> items, String nextCursor) {
    }
}
