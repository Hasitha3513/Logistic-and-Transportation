package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuelIssueRepository {
    FuelIssue save(FuelIssue issue);

    Optional<FuelIssue> findById(UUID id);

    Optional<FuelIssue> findByIdForUpdate(UUID id);

    FuelIssueUseCase.PageResult<FuelIssue> search(FuelIssueUseCase.SearchQuery query);

    List<FuelIssue> findByTripId(UUID tripId);

    boolean existsByVoucherNumber(String voucherNumber);
}
