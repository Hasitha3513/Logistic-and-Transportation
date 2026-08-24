package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Component
class FuelIssuePersistenceAdapter implements FuelIssueRepository {
    private final FuelIssueJpaRepository repository;

    FuelIssuePersistenceAdapter(FuelIssueJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public FuelIssue save(FuelIssue issue) {
        return map(repository.save(entity(issue)));
    }

    @Override
    public Optional<FuelIssue> findById(UUID id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    public Optional<FuelIssue> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(this::map);
    }

    @Override
    public java.util.List<FuelIssue> findByTripId(UUID tripId) {
        if (tripId == null) return java.util.List.of();
        return repository.findByTripIdOrderByIssueDateTimeAsc(tripId).stream().map(this::map).toList();
    }

    @Override
    public FuelIssueUseCase.PageResult<FuelIssue> search(FuelIssueUseCase.SearchQuery request) {
        var specification = (org.springframework.data.jpa.domain.Specification<FuelIssueEntity>) (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (request.vehicleId() != null) predicates.add(builder.equal(root.get("vehicleId"), request.vehicleId()));
            if (request.tripId() != null) predicates.add(builder.equal(root.get("tripId"), request.tripId()));
            if (request.status() != null) predicates.add(builder.equal(root.get("status"), request.status()));
            if (request.fromDate() != null) predicates.add(builder.greaterThanOrEqualTo(root.get("issueDateTime"),
                    request.fromDate().atStartOfDay().atOffset(ZoneOffset.UTC)));
            if (request.toDate() != null) predicates.add(builder.lessThan(root.get("issueDateTime"),
                    request.toDate().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
            if (request.voucherNumber() != null && !request.voucherNumber().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("voucherNumber")),
                        "%" + request.voucherNumber().trim().toLowerCase() + "%"));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        var page = repository.findAll(specification, PageRequest.of(request.page(), request.limit(),
                Sort.by(Sort.Direction.DESC, "issueDateTime")));
        return new FuelIssueUseCase.PageResult<>(page.getContent().stream().map(this::map).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public boolean existsByVoucherNumber(String voucherNumber) {
        return repository.existsByVoucherNumber(voucherNumber);
    }

    private FuelIssueEntity entity(FuelIssue issue) {
        var entity = new FuelIssueEntity();
        entity.setId(issue.id());
        entity.setVoucherNumber(issue.voucherNumber());
        entity.setVehicleId(issue.vehicleId());
        entity.setTripId(issue.tripId());
        entity.setDriverId(issue.driverId());
        entity.setFuelType(issue.fuelType());
        entity.setQuantity(issue.quantity());
        entity.setUnitPrice(issue.unitPrice());
        entity.setTotalAmount(issue.totalAmount());
        entity.setStationId(issue.stationId());
        entity.setOdometer(issue.odometer());
        entity.setEngineHours(issue.engineHours());
        entity.setIssueDateTime(issue.issueDateTime());
        entity.setStatus(issue.status());
        entity.setRequestedBy(issue.requestedBy());
        entity.setAuthorizedBy(issue.authorizedBy());
        entity.setAuthorizationDateTime(issue.authorizationDateTime());
        entity.setNotes(issue.notes());
        entity.setCreatedAt(issue.createdAt());
        entity.setUpdatedAt(issue.updatedAt());
        return entity;
    }

    private FuelIssue map(FuelIssueEntity entity) {
        return new FuelIssue(entity.getId(), entity.getVoucherNumber(), entity.getVehicleId(), entity.getTripId(),
                entity.getDriverId(), entity.getFuelType(), entity.getQuantity(), entity.getUnitPrice(),
                entity.getTotalAmount(), entity.getStationId(), entity.getOdometer(), entity.getEngineHours(),
                entity.getIssueDateTime(), entity.getStatus(), entity.getRequestedBy(), entity.getAuthorizedBy(),
                entity.getAuthorizationDateTime(), entity.getNotes(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
