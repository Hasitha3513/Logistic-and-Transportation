package com.transportlogistics.app.fleet.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverViolation(
        UUID id,
        UUID driverId,
        UUID tripId,
        DriverViolationType violationType,
        ViolationSeverity severity,
        OffsetDateTime violationDate,
        int penaltyPoints,
        BigDecimal fineAmount,
        FinePaymentStatus paymentStatus,
        OffsetDateTime paidAt,
        String paymentReference,
        String location,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public DriverViolation {
        if (driverId == null) {
            throw new BusinessRuleException("INVALID_DRIVER_ID", "Driver id cannot be null");
        }
        if (violationType == null) {
            throw new BusinessRuleException("INVALID_VIOLATION_TYPE", "Violation type cannot be null");
        }
        if (severity == null) {
            throw new BusinessRuleException("INVALID_SEVERITY", "Violation severity cannot be null");
        }
        if (violationDate == null) {
            throw new BusinessRuleException("INVALID_VIOLATION_DATE", "Violation date cannot be null");
        }
        if (penaltyPoints < 0) {
            throw new BusinessRuleException("INVALID_PENALTY_POINTS", "Penalty points cannot be negative");
        }
        if (fineAmount == null || fineAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_FINE_AMOUNT", "Fine amount cannot be negative");
        }
        if (paymentStatus == null) {
            throw new BusinessRuleException("INVALID_PAYMENT_STATUS", "Payment status cannot be null");
        }
        if (paymentStatus == FinePaymentStatus.PAID && paidAt == null) {
            throw new BusinessRuleException("PAYMENT_TIMESTAMP_REQUIRED", "Paid violation must have a payment timestamp");
        }
    }

    public static DriverViolation record(
            UUID driverId,
            UUID tripId,
            DriverViolationType violationType,
            ViolationSeverity severity,
            OffsetDateTime violationDate,
            int penaltyPoints,
            BigDecimal fineAmount,
            String location,
            String description,
            String recordedBy
    ) {
        var now = OffsetDateTime.now();
        return new DriverViolation(
                UUID.randomUUID(),
                driverId,
                tripId,
                violationType,
                severity,
                violationDate,
                penaltyPoints,
                fineAmount != null ? fineAmount : BigDecimal.ZERO,
                FinePaymentStatus.UNPAID,
                null,
                null,
                location,
                description,
                now,
                now,
                recordedBy,
                recordedBy
        );
    }

    public DriverViolation pay(OffsetDateTime paidAt, String paymentReference, String updatedBy) {
        if (paymentStatus == FinePaymentStatus.PAID) {
            throw new BusinessRuleException("ALREADY_PAID", "Fine is already paid");
        }
        if (paymentStatus == FinePaymentStatus.WAIVED) {
            throw new BusinessRuleException("FINE_WAIVED", "Cannot pay a waived fine");
        }
        var timestamp = paidAt != null ? paidAt : OffsetDateTime.now();
        var ref = paymentReference != null && !paymentReference.isBlank() ? paymentReference.trim() : "PAID";
        return new DriverViolation(
                id,
                driverId,
                tripId,
                violationType,
                severity,
                violationDate,
                penaltyPoints,
                fineAmount,
                FinePaymentStatus.PAID,
                timestamp,
                ref,
                location,
                description,
                createdAt,
                OffsetDateTime.now(),
                createdBy,
                updatedBy
        );
    }

    public DriverViolation waive(String reason, String updatedBy) {
        if (paymentStatus == FinePaymentStatus.PAID) {
            throw new BusinessRuleException("ALREADY_PAID", "Cannot waive an already paid fine");
        }
        if (paymentStatus == FinePaymentStatus.WAIVED) {
            throw new BusinessRuleException("ALREADY_WAIVED", "Fine is already waived");
        }
        var updatedRemarks = description != null ? description + " [WAIVED: " + reason + "]" : "[WAIVED: " + reason + "]";
        return new DriverViolation(
                id,
                driverId,
                tripId,
                violationType,
                severity,
                violationDate,
                penaltyPoints,
                fineAmount,
                FinePaymentStatus.WAIVED,
                null,
                paymentReference,
                location,
                updatedRemarks,
                createdAt,
                OffsetDateTime.now(),
                createdBy,
                updatedBy
        );
    }

    public DriverViolation dispute(String reason, String updatedBy) {
        if (paymentStatus == FinePaymentStatus.PAID) {
            throw new BusinessRuleException("ALREADY_PAID", "Cannot dispute an already paid fine");
        }
        if (paymentStatus == FinePaymentStatus.WAIVED) {
            throw new BusinessRuleException("ALREADY_WAIVED", "Cannot dispute a waived fine");
        }
        var updatedRemarks = description != null ? description + " [DISPUTED: " + reason + "]" : "[DISPUTED: " + reason + "]";
        return new DriverViolation(
                id,
                driverId,
                tripId,
                violationType,
                severity,
                violationDate,
                penaltyPoints,
                fineAmount,
                FinePaymentStatus.DISPUTED,
                null,
                paymentReference,
                location,
                updatedRemarks,
                createdAt,
                OffsetDateTime.now(),
                createdBy,
                updatedBy
        );
    }
}
