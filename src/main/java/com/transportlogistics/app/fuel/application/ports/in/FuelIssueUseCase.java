package com.transportlogistics.app.fuel.application.ports.in;

import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueHistory;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FuelIssueUseCase {
    FuelIssue create(CreateCommand command, String actor);

    FuelIssue update(UUID id, UpdateCommand command, String actor);

    FuelIssue submit(UUID id, String actor);

    FuelIssue authorize(UUID id, String comment, String actor);

    FuelIssue issue(UUID id, String actor);

    FuelIssue cancel(UUID id, String reason, String actor);

    FuelIssue get(UUID id);

    PageResult<FuelIssue> search(SearchQuery query);

    List<FuelIssueHistory> history(UUID id);

    record CreateCommand(UUID vehicleId, UUID tripId, UUID driverId, String fuelType, BigDecimal quantity,
                         BigDecimal unitPrice, UUID stationId, BigDecimal odometer, BigDecimal engineHours,
                         OffsetDateTime issueDateTime, String notes) {
    }

    record UpdateCommand(UUID vehicleId, UUID tripId, UUID driverId, String fuelType, BigDecimal quantity,
                         BigDecimal unitPrice, UUID stationId, BigDecimal odometer, BigDecimal engineHours,
                         OffsetDateTime issueDateTime, String notes) {
    }

    record SearchQuery(int page, int limit, UUID vehicleId, UUID tripId, FuelIssueStatus status,
                       LocalDate fromDate, LocalDate toDate, String voucherNumber) {
        public SearchQuery {
            page = Math.max(0, page);
            limit = Math.min(100, Math.max(1, limit));
        }
    }

    record PageResult<T>(List<T> content, int page, int limit, long totalElements, int totalPages) {
    }
}
