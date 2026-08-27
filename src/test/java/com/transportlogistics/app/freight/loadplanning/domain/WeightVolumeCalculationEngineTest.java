package com.transportlogistics.app.freight.loadplanning.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeightVolumeCalculationEngineTest {

    @Test
    @DisplayName("Exact payload capacity returns PASS")
    void exactPayloadReturnsPass() {
        var vehicle = new WeightVolumeCalculationEngine.VehicleCapacityInput(
                5000.0, 3000.0, 8000.0, 20.0, 2, 4000.0
        );
        var items = List.of(
                new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        BigDecimal.valueOf(5), BigDecimal.valueOf(1000), WeightVolumeCalculationEngine.WeightUnit.KG,
                        BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(2), WeightVolumeCalculationEngine.DimensionUnit.M
                )
        );

        var result = WeightVolumeCalculationEngine.evaluate(vehicle, items);

        assertThat(result.cargoWeightKg()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.payloadCapacityKg()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.payloadUtilizationPercent()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.PASS);
        assertThat(result.gvwResult()).isEqualTo(ValidationOutcome.PASS);
    }

    @Test
    @DisplayName("Below payload capacity returns PASS with utilization percentage")
    void belowPayloadReturnsPass() {
        var vehicle = new WeightVolumeCalculationEngine.VehicleCapacityInput(
                5000.0, 3000.0, 8000.0, 20.0, 2, 4000.0
        );
        var items = List.of(
                new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        BigDecimal.valueOf(2), BigDecimal.valueOf(1000), WeightVolumeCalculationEngine.WeightUnit.KG,
                        BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), WeightVolumeCalculationEngine.DimensionUnit.M
                )
        );

        var result = WeightVolumeCalculationEngine.evaluate(vehicle, items);

        assertThat(result.cargoWeightKg()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(result.payloadUtilizationPercent()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.PASS);
    }

    @Test
    @DisplayName("Above payload capacity returns FAIL with VEHICLE_PAYLOAD_EXCEEDED violation")
    void abovePayloadReturnsFail() {
        var vehicle = new WeightVolumeCalculationEngine.VehicleCapacityInput(
                5000.0, 3000.0, 8000.0, 20.0, 2, 4000.0
        );
        var items = List.of(
                new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        BigDecimal.valueOf(6), BigDecimal.valueOf(1000), WeightVolumeCalculationEngine.WeightUnit.KG,
                        BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), WeightVolumeCalculationEngine.DimensionUnit.M
                )
        );

        var result = WeightVolumeCalculationEngine.evaluate(vehicle, items);

        assertThat(result.cargoWeightKg()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(result.payloadUtilizationPercent()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.FAIL);
        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.FAIL);
        assertThat(result.violations()).anyMatch(v -> "VEHICLE_PAYLOAD_EXCEEDED".equals(v.code()));
    }

    @Test
    @DisplayName("Exact volume capacity returns PASS")
    void exactVolumeReturnsPass() {
        var vehicle = new WeightVolumeCalculationEngine.VehicleCapacityInput(
                10000.0, 3000.0, 13000.0, 10.0, 2, 4000.0
        );
        var items = List.of(
                new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        BigDecimal.valueOf(10), BigDecimal.valueOf(100), WeightVolumeCalculationEngine.WeightUnit.KG,
                        BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), WeightVolumeCalculationEngine.DimensionUnit.M
                )
        );

        var result = WeightVolumeCalculationEngine.evaluate(vehicle, items);

        assertThat(result.cargoVolumeM3()).isEqualByComparingTo(new BigDecimal("10.000"));
        assertThat(result.volumeCapacityM3()).isEqualByComparingTo(new BigDecimal("10.000"));
        assertThat(result.volumeUtilizationPercent()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.volumeResult()).isEqualTo(ValidationOutcome.PASS);
    }

    @Test
    @DisplayName("Above volume capacity returns FAIL with VEHICLE_VOLUME_CAPACITY_EXCEEDED violation")
    void aboveVolumeReturnsFail() {
        var vehicle = new WeightVolumeCalculationEngine.VehicleCapacityInput(
                10000.0, 3000.0, 13000.0, 10.0, 2, 4000.0
        );
        var items = List.of(
                new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        BigDecimal.valueOf(12), BigDecimal.valueOf(100), WeightVolumeCalculationEngine.WeightUnit.KG,
                        BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), WeightVolumeCalculationEngine.DimensionUnit.M
                )
        );

        var result = WeightVolumeCalculationEngine.evaluate(vehicle, items);

        assertThat(result.cargoVolumeM3()).isEqualByComparingTo(new BigDecimal("12.000"));
        assertThat(result.volumeUtilizationPercent()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(result.volumeResult()).isEqualTo(ValidationOutcome.FAIL);
        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.FAIL);
        assertThat(result.violations()).anyMatch(v -> "VEHICLE_VOLUME_CAPACITY_EXCEEDED".equals(v.code()));
    }

    @Test
    @DisplayName("Projected gross weight exceeding GVW limit returns FAIL with VEHICLE_GVW_EXCEEDED")
    void aboveGvwReturnsFail() {
        var vehicle = new WeightVolumeCalculationEngine.VehicleCapacityInput(
                6000.0, 3500.0, 8000.0, 25.0, 2, 4500.0
        );
        // Cargo weight = 5000 kg <= payload capacity 6000 kg (Payload PASS)
        // Projected Gross Weight = 3500 + 5000 = 8500 kg > GVW limit 8000 kg (GVW FAIL)
        var items = List.of(
                new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        BigDecimal.valueOf(5), BigDecimal.valueOf(1000), WeightVolumeCalculationEngine.WeightUnit.KG,
                        BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), WeightVolumeCalculationEngine.DimensionUnit.M
                )
        );

        var result = WeightVolumeCalculationEngine.evaluate(vehicle, items);

        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.PASS);
        assertThat(result.projectedGrossWeightKg()).isEqualByComparingTo(new BigDecimal("8500.00"));
        assertThat(result.grossWeightLimitKg()).isEqualByComparingTo(new BigDecimal("8000.00"));
        assertThat(result.gvwResult()).isEqualTo(ValidationOutcome.FAIL);
        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.FAIL);
        assertThat(result.violations()).anyMatch(v -> "VEHICLE_GVW_EXCEEDED".equals(v.code()));
    }

    @Test
    @DisplayName("Multiple lines with different units aggregate correctly with unit normalization")
    void unitNormalizationAndAggregation() {
        var vehicle = new WeightVolumeCalculationEngine.VehicleCapacityInput(
                10000.0, 4000.0, 14000.0, 30.0, 2, 5000.0
        );
        var items = List.of(
                // 10 units x 500 grams = 5 kg; dimensions 100 cm x 50 cm x 20 cm = 0.1 m3 each => 1.0 m3
                new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        BigDecimal.valueOf(10), BigDecimal.valueOf(500), WeightVolumeCalculationEngine.WeightUnit.G,
                        BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(20), WeightVolumeCalculationEngine.DimensionUnit.CM
                ),
                // 2 units x 1.5 tonnes = 3000 kg; dimensions 2000 mm x 1000 mm x 1000 mm = 2.0 m3 each => 4.0 m3
                new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        BigDecimal.valueOf(2), new BigDecimal("1.5"), WeightVolumeCalculationEngine.WeightUnit.TONNE,
                        BigDecimal.valueOf(2000), BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), WeightVolumeCalculationEngine.DimensionUnit.MM
                )
        );

        var result = WeightVolumeCalculationEngine.evaluate(vehicle, items);

        assertThat(result.cargoWeightKg()).isEqualByComparingTo(new BigDecimal("3005.00"));
        assertThat(result.cargoVolumeM3()).isEqualByComparingTo(new BigDecimal("5.000"));
        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.PASS);
        assertThat(result.volumeResult()).isEqualTo(ValidationOutcome.PASS);
        assertThat(result.gvwResult()).isEqualTo(ValidationOutcome.PASS);
        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.PASS);
    }

    @Test
    @DisplayName("Unknown vehicle capacity master data returns INCOMPLETE without false zeroing")
    void unknownVehicleCapacityReturnsIncomplete() {
        var vehicle = new WeightVolumeCalculationEngine.VehicleCapacityInput(
                null, null, null, null, null, null
        );
        var items = List.of(
                new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        BigDecimal.valueOf(1), BigDecimal.valueOf(100), WeightVolumeCalculationEngine.WeightUnit.KG,
                        BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), WeightVolumeCalculationEngine.DimensionUnit.M
                )
        );

        var result = WeightVolumeCalculationEngine.evaluate(vehicle, items);

        assertThat(result.payloadCapacityKg()).isNull();
        assertThat(result.volumeCapacityM3()).isNull();
        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.volumeResult()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.gvwResult()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.missingData()).contains(
                "VEHICLE_PAYLOAD_CAPACITY_MISSING",
                "VEHICLE_VOLUME_CAPACITY_UNAVAILABLE",
                "VEHICLE_GVW_DATA_MISSING"
        );
    }

    @Test
    @DisplayName("Unknown cargo measurements returns INCOMPLETE with CARGO_ITEM_WEIGHT_DATA_MISSING")
    void unknownCargoMeasurementsReturnsIncomplete() {
        var vehicle = new WeightVolumeCalculationEngine.VehicleCapacityInput(
                5000.0, 3000.0, 8000.0, 20.0, 2, 4000.0
        );
        var items = List.of(
                new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        BigDecimal.valueOf(5), null, WeightVolumeCalculationEngine.WeightUnit.KG,
                        null, null, null, WeightVolumeCalculationEngine.DimensionUnit.M
                )
        );

        var result = WeightVolumeCalculationEngine.evaluate(vehicle, items);

        assertThat(result.cargoWeightKg()).isNull();
        assertThat(result.cargoVolumeM3()).isNull();
        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.volumeResult()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.missingData()).contains(
                "CARGO_ITEM_WEIGHT_DATA_MISSING",
                "CARGO_ITEM_DIMENSIONS_DATA_MISSING"
        );
    }
}
