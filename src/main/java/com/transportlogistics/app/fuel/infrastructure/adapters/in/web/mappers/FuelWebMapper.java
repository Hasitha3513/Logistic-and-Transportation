package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueHistory;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelIssueHistoryResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelIssueResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelStationResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.Reference;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface FuelWebMapper {

    FuelStationResponse toResponse(FuelStation station);
    List<FuelStationResponse> toFuelStationResponseList(List<FuelStation> stations);

    FuelIssueHistoryResponse toResponse(FuelIssueHistory history);
    List<FuelIssueHistoryResponse> toFuelIssueHistoryResponseList(List<FuelIssueHistory> histories);

    default FuelIssueResponse toResponse(FuelIssue issue, FuelStation station) {
        if (issue == null) return null;
        return new FuelIssueResponse(
                issue.id(),
                issue.voucherNumber(),
                new Reference(issue.vehicleId()),
                nullableReference(issue.tripId()),
                nullableReference(issue.driverId()),
                issue.fuelType(),
                issue.quantity(),
                issue.unitPrice(),
                issue.totalAmount(),
                toResponse(station),
                issue.odometer(),
                issue.engineHours(),
                issue.issueDateTime(),
                issue.status(),
                issue.requestedBy(),
                issue.authorizedBy(),
                issue.authorizationDateTime(),
                issue.notes(),
                issue.createdAt(),
                issue.updatedAt()
        );
    }

    default Reference nullableReference(UUID id) {
        return id == null ? null : new Reference(id);
    }
}
