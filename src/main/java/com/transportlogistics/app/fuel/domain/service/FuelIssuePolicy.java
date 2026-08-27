package com.transportlogistics.app.fuel.domain.service;

import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelLimitPolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class FuelIssuePolicy {
    public void validateDraft(FuelIssue issue) {
        if (issue.vehicleId() == null) validation("FUEL_VEHICLE_REQUIRED", "Vehicle is required");
        if (issue.stationId() == null) validation("FUEL_STATION_REQUIRED", "Fuel station is required");
        if (issue.issueDateTime() == null) validation("FUEL_ISSUE_DATE_REQUIRED", "Issue date and time are required");
        if (issue.fuelType() == null || issue.fuelType().isBlank()) {
            validation("FUEL_TYPE_REQUIRED", "Fuel type is required");
        }
        if (issue.quantity() == null || issue.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            validation("INVALID_FUEL_QUANTITY", "Fuel quantity must be greater than zero");
        }
        if (issue.unitPrice() != null && issue.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            validation("INVALID_FUEL_PRICE", "Fuel unit price cannot be negative");
        }
        nonNegative(issue.odometer(), "INVALID_FUEL_ODOMETER", "Odometer cannot be negative");
        nonNegative(issue.engineHours(), "INVALID_FUEL_ENGINE_HOURS", "Engine hours cannot be negative");
    }

    public void enforceLimits(FuelIssue issue, List<FuelLimitPolicy> policies) {
        policies.stream().filter(FuelLimitPolicy::active)
                .filter(policy -> policy.vehicleId() == null || policy.vehicleId().equals(issue.vehicleId()))
                .sorted(Comparator.comparing((FuelLimitPolicy policy) -> policy.vehicleId() == null ? 1 : 0))
                .findFirst().ifPresent(policy -> {
                    if (policy.maximumQuantityPerIssue() != null
                            && issue.quantity().compareTo(policy.maximumQuantityPerIssue()) > 0) {
                        validation("FUEL_LIMIT_EXCEEDED", "Fuel quantity exceeds the configured per-issue limit of "
                                + policy.maximumQuantityPerIssue());
                    }
                });
    }

    public void requireEditable(FuelIssue issue) {
        if (issue.status() != FuelIssueStatus.DRAFT) {
            conflict("FUEL_ISSUE_NOT_EDITABLE", "Only a DRAFT fuel issue can be edited");
        }
    }

    public void requireSubmittable(FuelIssue issue) {
        if (issue.status() != FuelIssueStatus.DRAFT) {
            conflict("FUEL_ISSUE_NOT_SUBMITTABLE", "Submit requires a DRAFT fuel issue");
        }
    }

    public void requireAuthorizable(FuelIssue issue) {
        if (issue.status() != FuelIssueStatus.PENDING_AUTHORIZATION) {
            conflict("FUEL_ISSUE_NOT_AUTHORIZABLE", "Authorize requires a PENDING_AUTHORIZATION fuel issue");
        }
    }

    public void requireIssuable(FuelIssue issue) {
        if (issue.status() != FuelIssueStatus.AUTHORIZED) {
            conflict("FUEL_ISSUE_NOT_ISSUABLE", "Issue requires an AUTHORIZED fuel issue");
        }
    }

    public void requireCancellable(FuelIssue issue, String reason) {
        if (issue.status() == FuelIssueStatus.ISSUED || issue.status() == FuelIssueStatus.CANCELLED) {
            conflict("FUEL_ISSUE_NOT_CANCELLABLE", "Fuel issue cannot be cancelled from status " + issue.status());
        }
        if (issue.status() != FuelIssueStatus.DRAFT && (reason == null || reason.isBlank())) {
            validation("FUEL_CANCELLATION_REASON_REQUIRED", "Cancellation reason is required after submission");
        }
    }

    private void nonNegative(BigDecimal value, String code, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) validation(code, message);
    }

    private void validation(String code, String message) {
        throw new BusinessRuleException(code, message);
    }

    private void conflict(String code, String message) {
        throw new ConflictException(code, message);
    }
}
