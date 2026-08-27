package com.transportlogistics.app.freight.loadplanning.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure domain calculation and validation engine for US-27 (Weight, Volume, and Capacity Validation).
 *
 * <p>Contains no framework or persistence dependencies.</p>
 */
public final class WeightVolumeCalculationEngine {

    public enum WeightUnit {
        KG(BigDecimal.ONE),
        G(new BigDecimal("0.001")),
        TONNE(new BigDecimal("1000"));

        private final BigDecimal toKgMultiplier;

        WeightUnit(BigDecimal toKgMultiplier) {
            this.toKgMultiplier = toKgMultiplier;
        }

        public BigDecimal toKg(BigDecimal value) {
            if (value == null) return null;
            return value.multiply(toKgMultiplier);
        }
    }

    public enum DimensionUnit {
        M(BigDecimal.ONE),
        CM(new BigDecimal("0.01")),
        MM(new BigDecimal("0.001"));

        private final BigDecimal toMeterMultiplier;

        DimensionUnit(BigDecimal toMeterMultiplier) {
            this.toMeterMultiplier = toMeterMultiplier;
        }

        public BigDecimal toMeters(BigDecimal value) {
            if (value == null) return null;
            return value.multiply(toMeterMultiplier);
        }
    }

    public record CargoLineMeasurement(
            BigDecimal quantity,
            BigDecimal unitWeight,
            WeightUnit weightUnit,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            DimensionUnit dimensionUnit
    ) {
        public CargoLineMeasurement(BigDecimal quantity, BigDecimal unitWeightKg, BigDecimal unitVolumeM3) {
            this(quantity, unitWeightKg, WeightUnit.KG, unitVolumeM3, BigDecimal.ONE, BigDecimal.ONE, DimensionUnit.M);
        }

        public BigDecimal lineWeightKg() {
            if (quantity == null || unitWeight == null || weightUnit == null) {
                return null;
            }
            return quantity.multiply(weightUnit.toKg(unitWeight));
        }

        public BigDecimal lineVolumeM3() {
            if (quantity == null || length == null || width == null || height == null || dimensionUnit == null) {
                return null;
            }
            BigDecimal lengthM = dimensionUnit.toMeters(length);
            BigDecimal widthM = dimensionUnit.toMeters(width);
            BigDecimal heightM = dimensionUnit.toMeters(height);
            BigDecimal unitVolume = lengthM.multiply(widthM).multiply(heightM);
            return quantity.multiply(unitVolume);
        }
    }

    public record VehicleCapacityInput(
            Double payloadCapacityKg,
            Double tareWeightKg,
            Double grossVehicleWeightKg,
            Double cargoVolumeCapacityM3,
            Integer axleCount,
            Double maxAxleLoadKg
    ) {}

    public record EvaluationResult(
            ValidationOutcome overallOutcome,
            BigDecimal cargoWeightKg,
            BigDecimal payloadCapacityKg,
            BigDecimal payloadUtilizationPercent,
            BigDecimal cargoVolumeM3,
            BigDecimal volumeCapacityM3,
            BigDecimal volumeUtilizationPercent,
            BigDecimal projectedGrossWeightKg,
            BigDecimal grossWeightLimitKg,
            BigDecimal tareWeightKg,
            ValidationOutcome payloadResult,
            ValidationOutcome volumeResult,
            ValidationOutcome gvwResult,
            ValidationOutcome axleResult,
            List<LoadValidationViolation> violations,
            List<String> missingData
    ) {}

    private WeightVolumeCalculationEngine() {}

    public static BigDecimal calculateTotalWeight(List<CargoLineMeasurement> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (CargoLineMeasurement item : items) {
            BigDecimal lineWeight = item.lineWeightKg();
            if (lineWeight == null) {
                return null; // missing weight data for at least one item
            }
            total = total.add(lineWeight);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateTotalVolume(List<CargoLineMeasurement> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (CargoLineMeasurement item : items) {
            BigDecimal lineVolume = item.lineVolumeM3();
            if (lineVolume == null) {
                return null; // missing dimension data for at least one item
            }
            total = total.add(lineVolume);
        }
        return total.setScale(3, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateProjectedGrossWeight(BigDecimal tareWeightKg, BigDecimal totalCargoWeightKg) {
        if (tareWeightKg == null || totalCargoWeightKg == null) {
            return null;
        }
        return tareWeightKg.add(totalCargoWeightKg).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateUtilizationPercent(BigDecimal actual, BigDecimal capacity) {
        if (actual == null || capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return actual.divide(capacity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static EvaluationResult evaluate(VehicleCapacityInput vehicle, List<CargoLineMeasurement> cargoItems) {
        List<LoadValidationViolation> violations = new ArrayList<>();
        List<String> missingData = new ArrayList<>();

        // 1. Cargo Measurements Aggregation
        BigDecimal cargoWeightKg = calculateTotalWeight(cargoItems);
        BigDecimal cargoVolumeM3 = calculateTotalVolume(cargoItems);

        if (cargoWeightKg == null) {
            missingData.add("CARGO_ITEM_WEIGHT_DATA_MISSING");
        }
        if (cargoVolumeM3 == null) {
            missingData.add("CARGO_ITEM_DIMENSIONS_DATA_MISSING");
        }

        // 2. Vehicle Master Data Mapping
        BigDecimal payloadCapacityKg = vehicle != null && vehicle.payloadCapacityKg() != null
                ? BigDecimal.valueOf(vehicle.payloadCapacityKg()).setScale(2, RoundingMode.HALF_UP) : null;
        BigDecimal tareWeightKg = vehicle != null && vehicle.tareWeightKg() != null
                ? BigDecimal.valueOf(vehicle.tareWeightKg()).setScale(2, RoundingMode.HALF_UP) : null;
        BigDecimal gvwLimitKg = vehicle != null && vehicle.grossVehicleWeightKg() != null
                ? BigDecimal.valueOf(vehicle.grossVehicleWeightKg()).setScale(2, RoundingMode.HALF_UP) : null;
        BigDecimal volumeCapacityM3 = vehicle != null && vehicle.cargoVolumeCapacityM3() != null
                ? BigDecimal.valueOf(vehicle.cargoVolumeCapacityM3()).setScale(3, RoundingMode.HALF_UP) : null;

        if (payloadCapacityKg == null) {
            missingData.add("VEHICLE_PAYLOAD_CAPACITY_MISSING");
        }
        if (volumeCapacityM3 == null) {
            missingData.add("VEHICLE_VOLUME_CAPACITY_UNAVAILABLE");
        }
        if (tareWeightKg == null || gvwLimitKg == null) {
            missingData.add("VEHICLE_GVW_DATA_MISSING");
        }
        missingData.add("VEHICLE_AXLE_LIMITS_UNAVAILABLE");

        // 3. Projected Gross Weight
        BigDecimal projectedGrossWeightKg = calculateProjectedGrossWeight(tareWeightKg, cargoWeightKg);

        // 4. Utilization Percentages
        BigDecimal payloadUtilization = calculateUtilizationPercent(cargoWeightKg, payloadCapacityKg);
        BigDecimal volumeUtilization = calculateUtilizationPercent(cargoVolumeM3, volumeCapacityM3);

        // 5. Payload Validation
        ValidationOutcome payloadResult;
        if (cargoWeightKg == null || payloadCapacityKg == null) {
            payloadResult = ValidationOutcome.INCOMPLETE;
            if (payloadCapacityKg == null) {
                violations.add(new LoadValidationViolation("VEHICLE_PAYLOAD_CAPACITY_UNAVAILABLE", "Vehicle payload capacity is unavailable"));
            }
            if (cargoWeightKg == null) {
                violations.add(new LoadValidationViolation("LOAD_WEIGHT_DATA_MISSING", "Cargo item weight measurements are unavailable to compute total cargo weight"));
            }
        } else if (cargoWeightKg.compareTo(payloadCapacityKg) > 0) {
            payloadResult = ValidationOutcome.FAIL;
            violations.add(new LoadValidationViolation(
                    "VEHICLE_PAYLOAD_EXCEEDED",
                    String.format("Cargo weight (%s kg) exceeds vehicle payload capacity (%s kg)", cargoWeightKg, payloadCapacityKg)
            ));
        } else {
            payloadResult = ValidationOutcome.PASS;
        }

        // 6. Volume Capacity Validation
        ValidationOutcome volumeResult;
        if (cargoVolumeM3 == null || volumeCapacityM3 == null) {
            volumeResult = ValidationOutcome.INCOMPLETE;
            if (volumeCapacityM3 == null) {
                violations.add(new LoadValidationViolation("VEHICLE_VOLUME_CAPACITY_UNAVAILABLE", "Vehicle cargo volume capacity is unavailable"));
            }
            if (cargoVolumeM3 == null) {
                violations.add(new LoadValidationViolation("LOAD_VOLUME_DATA_MISSING", "Cargo item dimensions are unavailable to compute cubic volume"));
            }
        } else if (cargoVolumeM3.compareTo(volumeCapacityM3) > 0) {
            volumeResult = ValidationOutcome.FAIL;
            violations.add(new LoadValidationViolation(
                    "VEHICLE_VOLUME_CAPACITY_EXCEEDED",
                    String.format("Cargo volume (%s m³) exceeds vehicle cargo volume capacity (%s m³)", cargoVolumeM3, volumeCapacityM3)
            ));
        } else {
            volumeResult = ValidationOutcome.PASS;
        }

        // 7. GVW Validation
        ValidationOutcome gvwResult;
        if (projectedGrossWeightKg == null || gvwLimitKg == null) {
            gvwResult = ValidationOutcome.INCOMPLETE;
            if (gvwLimitKg == null) {
                violations.add(new LoadValidationViolation("VEHICLE_GVW_DATA_UNAVAILABLE", "Vehicle gross vehicle weight limit is unavailable"));
            }
        } else if (projectedGrossWeightKg.compareTo(gvwLimitKg) > 0) {
            gvwResult = ValidationOutcome.FAIL;
            violations.add(new LoadValidationViolation(
                    "VEHICLE_GVW_EXCEEDED",
                    String.format("Projected gross weight (%s kg) exceeds vehicle GVW limit (%s kg)", projectedGrossWeightKg, gvwLimitKg)
            ));
        } else {
            gvwResult = ValidationOutcome.PASS;
        }

        // 8. Axle Validation (Gated as BLOCKED_PENDING_AXLE_DISTRIBUTION_CONTRACT per DATA-001)
        ValidationOutcome axleResult = ValidationOutcome.INCOMPLETE;
        violations.add(new LoadValidationViolation(
                "LOAD_AXLE_DATA_UNAVAILABLE",
                "Vehicle axle load distribution calculation is blocked pending 3D cargo distribution contract"
        ));

        // 9. Overall Outcome Evaluation
        ValidationOutcome overallOutcome;
        if (payloadResult == ValidationOutcome.FAIL || volumeResult == ValidationOutcome.FAIL || gvwResult == ValidationOutcome.FAIL) {
            overallOutcome = ValidationOutcome.FAIL;
        } else if (payloadResult == ValidationOutcome.INCOMPLETE || volumeResult == ValidationOutcome.INCOMPLETE || gvwResult == ValidationOutcome.INCOMPLETE) {
            overallOutcome = ValidationOutcome.INCOMPLETE;
        } else {
            overallOutcome = ValidationOutcome.PASS;
        }

        return new EvaluationResult(
                overallOutcome,
                cargoWeightKg,
                payloadCapacityKg,
                payloadUtilization,
                cargoVolumeM3,
                volumeCapacityM3,
                volumeUtilization,
                projectedGrossWeightKg,
                gvwLimitKg,
                tareWeightKg,
                payloadResult,
                volumeResult,
                gvwResult,
                axleResult,
                Collections.unmodifiableList(violations),
                Collections.unmodifiableList(missingData)
        );
    }
}
