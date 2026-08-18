package com.transportlogistics.app.fuel.domain;

import com.transportlogistics.app.fuel.domain.model.BunkerTank;
import com.transportlogistics.app.fuel.domain.model.BunkerTankStatus;
import com.transportlogistics.app.fuel.domain.policy.BunkerTankPolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BunkerTankPolicyTest {

    private BunkerTankPolicy policy;
    private BunkerTank dieselTank;
    private BunkerTank petrolTank;

    @BeforeEach
    void setUp() {
        policy = new BunkerTankPolicy();
        var now = OffsetDateTime.now();
        dieselTank = new BunkerTank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BNK-DSL-01",
                "Main Diesel Tank",
                "DIESEL",
                new BigDecimal("10000.000"),
                new BigDecimal("2000.000"),
                new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE,
                now,
                true,
                now,
                now
        );
        petrolTank = new BunkerTank(
                UUID.randomUUID(),
                dieselTank.fuelStationId(),
                "BNK-PET-01",
                "Main Petrol Tank",
                "PETROL_92",
                new BigDecimal("5000.000"),
                new BigDecimal("1000.000"),
                new BigDecimal("500.000"),
                BunkerTankStatus.ACTIVE,
                now,
                true,
                now,
                now
        );
    }

    @Test
    void shouldCalculateAvailableCapacityAndLowStock() {
        assertThat(dieselTank.availableCapacity()).isEqualByComparingTo("8000.000");
        assertThat(dieselTank.isLowStock()).isFalse();

        var lowTank = new BunkerTank(
                dieselTank.id(), dieselTank.fuelStationId(), dieselTank.tankCode(), dieselTank.tankName(),
                dieselTank.fuelType(), dieselTank.capacityLiters(), new BigDecimal("800.000"),
                dieselTank.minimumStockLiters(), dieselTank.status(), dieselTank.commissionedAt(),
                dieselTank.active(), dieselTank.createdAt(), dieselTank.updatedAt()
        );
        assertThat(lowTank.isLowStock()).isTrue();
    }

    @Test
    void shouldValidateReceivableQuantityWithinCapacity() {
        policy.validateReceivable(dieselTank, new BigDecimal("5000.000"), "DIESEL");
    }

    @Test
    void shouldRejectReceivableWhenExceedingCapacity() {
        assertThatThrownBy(() -> policy.validateReceivable(dieselTank, new BigDecimal("8500.000"), "DIESEL"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).code()).isEqualTo("BUNKER_CAPACITY_EXCEEDED"));
    }

    @Test
    void shouldRejectReceivableWhenFuelTypeMismatched() {
        assertThatThrownBy(() -> policy.validateReceivable(dieselTank, new BigDecimal("1000.000"), "PETROL_92"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).code()).isEqualTo("BUNKER_FUEL_TYPE_MISMATCH"));
    }

    @Test
    void shouldValidateIssuableQuantityWithinStock() {
        policy.validateIssuable(dieselTank, new BigDecimal("1500.000"), "DIESEL");
    }

    @Test
    void shouldRejectIssuableWhenExceedingStock() {
        assertThatThrownBy(() -> policy.validateIssuable(dieselTank, new BigDecimal("2500.000"), "DIESEL"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).code()).isEqualTo("INSUFFICIENT_BUNKER_STOCK"));
    }

    @Test
    void shouldValidateValidTransfer() {
        var secondDieselTank = new BunkerTank(
                UUID.randomUUID(), dieselTank.fuelStationId(), "BNK-DSL-02", "Secondary Diesel Tank",
                "DIESEL", new BigDecimal("5000.000"), new BigDecimal("500.000"), new BigDecimal("500.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(), true, OffsetDateTime.now(), OffsetDateTime.now()
        );

        policy.validateTransfer(dieselTank, secondDieselTank, new BigDecimal("1000.000"));
    }

    @Test
    void shouldRejectTransferBetweenDifferentFuelTypes() {
        assertThatThrownBy(() -> policy.validateTransfer(dieselTank, petrolTank, new BigDecimal("500.000")))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).code()).isEqualTo("BUNKER_FUEL_TYPE_MISMATCH"));
    }
}
