package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.domain.model.FuelIssueHistory;

import java.util.List;
import java.util.UUID;

public interface FuelIssueHistoryRepository {
    FuelIssueHistory save(FuelIssueHistory history);

    List<FuelIssueHistory> findByFuelIssueId(UUID fuelIssueId);
}
