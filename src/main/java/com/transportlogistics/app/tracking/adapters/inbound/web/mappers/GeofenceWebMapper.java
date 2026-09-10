package com.transportlogistics.app.tracking.adapters.inbound.web.mappers;

import com.transportlogistics.app.tracking.adapters.inbound.web.dto.request.GeofenceRequests;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.GeofenceResponses;
import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceAlertPolicy;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePolygon;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransition;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceRuleException;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.domain.geofence.VehicleGeofenceState;
import com.transportlogistics.app.tracking.domain.geofence.Wgs84Coordinate;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceManagementUseCase;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceQuery;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GeofenceWebMapper {
    default GeofenceManagementUseCase.CreateGeofence command(GeofenceRequests.Create request) {
        return new GeofenceManagementUseCase.CreateGeofence(request.name(), request.type(),
                polygon(request.polygon()), request.locationId(), policy(request.alertPolicy()));
    }

    default GeofenceManagementUseCase.UpdateGeofence command(GeofenceRequests.Update request) {
        return new GeofenceManagementUseCase.UpdateGeofence(request.name(), request.type(),
                polygon(request.polygon()), request.locationId(), policy(request.alertPolicy()));
    }

    default GeofenceResponses.Definition response(Geofence value) {
        return new GeofenceResponses.Definition(value.id(), value.name(), value.type().name(),
                value.polygon().vertices().stream().map(this::response).toList(), value.locationId(),
                new GeofenceResponses.AlertPolicy(value.alertPolicy().alertOnEntry(),
                        value.alertPolicy().alertOnExit()),
                value.lifecycle().name(), value.version(), value.createdAt(), value.updatedAt());
    }

    default GeofenceResponses.DefinitionPage response(GeofenceQuery.Page<Geofence> page) {
        return new GeofenceResponses.DefinitionPage(
                page.items().stream().map(this::response).toList(), page.page(), page.size(), page.total());
    }

    default GeofenceResponses.Membership response(VehicleGeofenceState value) {
        return new GeofenceResponses.Membership(value.geofenceId(), value.vehicleId(),
                value.stableState().name(), value.definitionVersion(),
                value.lastEvaluatedSourceTimestamp());
    }

    default GeofenceResponses.MembershipPage membershipResponse(
            GeofenceQuery.Page<VehicleGeofenceState> page) {
        return new GeofenceResponses.MembershipPage(
                page.items().stream().map(this::response).toList(), page.page(), page.size(), page.total());
    }

    default GeofenceResponses.Transition response(GeofenceTransition value) {
        return new GeofenceResponses.Transition(value.transitionId(), value.geofenceId(),
                value.vehicleId(), value.locationId(), value.geofenceType().name(),
                value.transitionType().name(), value.severity().name(), value.sourceTimestamp(),
                value.definitionVersion());
    }

    default GeofenceResponses.TransitionPage response(
            GeofenceQuery.CursorPage<GeofenceTransition> page) {
        return new GeofenceResponses.TransitionPage(
                page.items().stream().map(this::response).toList(), page.nextCursor());
    }

    default GeofencePolygon polygon(java.util.List<GeofenceRequests.Coordinate> values) {
        try {
            return GeofencePolygon.of(values.stream()
                    .map(value -> new Wgs84Coordinate(value.longitude(), value.latitude())).toList());
        } catch (GeofenceRuleException exception) {
            throw new BusinessRuleException(exception.code(), exception.getMessage());
        }
    }

    default GeofenceAlertPolicy policy(GeofenceRequests.AlertPolicy value) {
        return new GeofenceAlertPolicy(value.alertOnEntry(), value.alertOnExit());
    }

    private GeofenceResponses.Coordinate response(Wgs84Coordinate value) {
        return new GeofenceResponses.Coordinate(value.longitude(), value.latitude());
    }
}
