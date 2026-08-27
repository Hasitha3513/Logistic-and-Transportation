package com.transportlogistics.app.fuel.application.ports.in;

import com.transportlogistics.app.fuel.domain.model.BunkerStockMovement;
import com.transportlogistics.app.fuel.domain.model.BunkerTank;
import com.transportlogistics.app.fuel.domain.model.BunkerTankStatus;
import com.transportlogistics.app.fuel.domain.model.DipReading;
import com.transportlogistics.app.fuel.domain.model.StockAdjustment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BunkerTankUseCase {

    BunkerTank create(CreateTankCommand command, String actorName);

    BunkerTank update(UUID id, UpdateTankCommand command, String actorName);

    BunkerTank get(UUID id);

    List<BunkerTank> list(UUID fuelStationId, String fuelType, Boolean active);

    BunkerTank setOpeningBalance(UUID id, BigDecimal openingBalanceLiters, String reason, String actorName);

    List<BunkerStockMovement> listMovements(UUID tankId, int page, int size);

    long countMovements(UUID tankId);

    DipReading recordDipReading(UUID tankId, BigDecimal physicalQuantityLiters, String notes, String actorName);

    List<DipReading> listDipReadings(UUID tankId);

    StockAdjustment adjustStock(UUID tankId, BigDecimal quantityDeltaLiters, String reason, UUID sourceDipReadingId, String actorName);

    void transfer(TransferCommand command, String actorName);

    record CreateTankCommand(
            UUID fuelStationId,
            String tankCode,
            String tankName,
            String fuelType,
            BigDecimal capacityLiters,
            BigDecimal minimumStockLiters,
            BigDecimal openingBalanceLiters,
            OffsetDateTime commissionedAt
    ) {}

    record UpdateTankCommand(
            String tankName,
            BigDecimal minimumStockLiters,
            BunkerTankStatus status,
            Boolean active
    ) {}

    record TransferCommand(
            UUID sourceTankId,
            UUID destinationTankId,
            BigDecimal quantityLiters,
            String reason
    ) {}
}
