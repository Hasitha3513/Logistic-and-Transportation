package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.BunkerTankUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.fuel.domain.policy.BunkerTankPolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BunkerTankService implements BunkerTankUseCase {

    private final BunkerTankRepository tanks;
    private final BunkerStockLedgerRepository movements;
    private final DipReadingRepository dipReadings;
    private final StockAdjustmentRepository adjustments;
    private final FuelStationRepository stations;
    private final FuelActorPort actors;
    private final FuelTransaction transactions;
    private final BunkerTankPolicy policy = new BunkerTankPolicy();
    private final Clock clock;

    public BunkerTankService(
            BunkerTankRepository tanks,
            BunkerStockLedgerRepository movements,
            DipReadingRepository dipReadings,
            StockAdjustmentRepository adjustments,
            FuelStationRepository stations,
            FuelActorPort actors,
            FuelTransaction transactions,
            Clock clock
    ) {
        this.tanks = tanks;
        this.movements = movements;
        this.dipReadings = dipReadings;
        this.adjustments = adjustments;
        this.stations = stations;
        this.actors = actors;
        this.transactions = transactions;
        this.clock = clock;
    }

    private FuelActorPort.Actor actor(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessRuleException("UNAUTHORIZED_ACTOR", "Authenticated actor is required");
        }
        return actors.find(username.trim())
                .orElseThrow(() -> new BusinessRuleException("ACTOR_NOT_FOUND", "Actor not found for username: " + username));
    }

    @Override
    public BunkerTank create(CreateTankCommand command, String actorName) {
        return transactions.execute(() -> {
            var actor = actor(actorName);
            var now = OffsetDateTime.now(clock);

            if (command.fuelStationId() == null) {
                throw new BusinessRuleException("INVALID_BUNKER_TANK", "Fuel station ID is required");
            }
            var station = stations.findById(command.fuelStationId())
                    .orElseThrow(() -> new BusinessRuleException("FUEL_STATION_NOT_FOUND", "Fuel station not found: " + command.fuelStationId()));
            if (station.stationType() != FuelStationType.INTERNAL) {
                throw new BusinessRuleException("INVALID_BUNKER_STATION", "Bunker tanks can only be configured for INTERNAL fuel stations");
            }

            if (command.tankCode() == null || command.tankCode().trim().isBlank()) {
                throw new BusinessRuleException("INVALID_BUNKER_TANK", "Tank code is required");
            }
            if (command.tankName() == null || command.tankName().trim().isBlank()) {
                throw new BusinessRuleException("INVALID_BUNKER_TANK", "Tank name is required");
            }
            if (command.fuelType() == null || command.fuelType().trim().isBlank()) {
                throw new BusinessRuleException("INVALID_BUNKER_TANK", "Fuel type is required");
            }
            if (command.capacityLiters() == null || command.capacityLiters().signum() <= 0) {
                throw new BusinessRuleException("INVALID_BUNKER_TANK", "Capacity must be greater than zero");
            }

            String tankCode = command.tankCode().trim().toUpperCase();
            if (tanks.findByTankCode(tankCode).isPresent()) {
                throw new BusinessRuleException("DUPLICATE_BUNKER_TANK_CODE", "A tank with code " + tankCode + " already exists");
            }

            BigDecimal minStock = command.minimumStockLiters() != null && command.minimumStockLiters().signum() >= 0
                    ? command.minimumStockLiters().setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP);

            BigDecimal openingBalance = command.openingBalanceLiters() != null && command.openingBalanceLiters().signum() > 0
                    ? command.openingBalanceLiters().setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP);

            if (openingBalance.compareTo(command.capacityLiters()) > 0) {
                throw new BusinessRuleException("BUNKER_CAPACITY_EXCEEDED", "Opening balance cannot exceed tank capacity");
            }

            OffsetDateTime commissionedAt = command.commissionedAt() != null ? command.commissionedAt() : now;
            var tank = new BunkerTank(
                    UUID.randomUUID(),
                    command.fuelStationId(),
                    tankCode,
                    command.tankName().trim(),
                    command.fuelType().trim().toUpperCase(),
                    command.capacityLiters().setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP),
                    openingBalance,
                    minStock,
                    BunkerTankStatus.ACTIVE,
                    commissionedAt,
                    true,
                    now,
                    now
            );

            var saved = tanks.save(tank);

            if (openingBalance.signum() > 0) {
                movements.save(new BunkerStockMovement(
                        UUID.randomUUID(),
                        saved.id(),
                        BunkerMovementType.OPENING_BALANCE,
                        openingBalance,
                        openingBalance,
                        BunkerReferenceType.INITIAL_SETUP,
                        saved.id(),
                        now,
                        actor.id(),
                        "Initial tank commissioning balance",
                        now
                ));
            }

            return saved;
        });
    }

    @Override
    public BunkerTank update(UUID id, UpdateTankCommand command, String actorName) {
        return transactions.execute(() -> {
            var current = tanks.findById(id)
                    .orElseThrow(() -> new BusinessRuleException("BUNKER_TANK_NOT_FOUND", "Bunker tank not found: " + id));
            var now = OffsetDateTime.now(clock);

            String tankName = command.tankName() != null && !command.tankName().trim().isBlank()
                    ? command.tankName().trim()
                    : current.tankName();

            BigDecimal minStock = command.minimumStockLiters() != null && command.minimumStockLiters().signum() >= 0
                    ? command.minimumStockLiters().setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP)
                    : current.minimumStockLiters();

            BunkerTankStatus status = command.status() != null ? command.status() : current.status();
            boolean active = command.active() != null ? command.active() : current.active();

            var updated = new BunkerTank(
                    current.id(),
                    current.fuelStationId(),
                    current.tankCode(),
                    tankName,
                    current.fuelType(),
                    current.capacityLiters(),
                    current.currentStockLiters(),
                    minStock,
                    status,
                    current.commissionedAt(),
                    active,
                    current.createdAt(),
                    now
            );

            return tanks.save(updated);
        });
    }

    @Override
    public BunkerTank get(UUID id) {
        return tanks.findById(id)
                .orElseThrow(() -> new BusinessRuleException("BUNKER_TANK_NOT_FOUND", "Bunker tank not found: " + id));
    }

    @Override
    public List<BunkerTank> list(UUID fuelStationId, String fuelType, Boolean active) {
        return tanks.list(fuelStationId, fuelType, active);
    }

    @Override
    public BunkerTank setOpeningBalance(UUID id, BigDecimal openingBalanceLiters, String reason, String actorName) {
        return transactions.execute(() -> {
            var actor = actor(actorName);
            var now = OffsetDateTime.now(clock);
            var current = tanks.findByIdForUpdate(id)
                    .orElseThrow(() -> new BusinessRuleException("BUNKER_TANK_NOT_FOUND", "Bunker tank not found: " + id));

            if (current.currentStockLiters().signum() > 0) {
                throw new BusinessRuleException("OPENING_BALANCE_ALREADY_SET", "Tank already has an established stock balance");
            }
            if (openingBalanceLiters == null || openingBalanceLiters.signum() <= 0) {
                throw new BusinessRuleException("INVALID_BUNKER_QUANTITY", "Opening balance must be greater than zero");
            }
            if (openingBalanceLiters.compareTo(current.capacityLiters()) > 0) {
                throw new BusinessRuleException("BUNKER_CAPACITY_EXCEEDED", "Opening balance cannot exceed tank capacity");
            }

            BigDecimal balance = openingBalanceLiters.setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP);
            var updated = new BunkerTank(
                    current.id(),
                    current.fuelStationId(),
                    current.tankCode(),
                    current.tankName(),
                    current.fuelType(),
                    current.capacityLiters(),
                    balance,
                    current.minimumStockLiters(),
                    current.status(),
                    current.commissionedAt(),
                    current.active(),
                    current.createdAt(),
                    now
            );

            var saved = tanks.save(updated);

            movements.save(new BunkerStockMovement(
                    UUID.randomUUID(),
                    saved.id(),
                    BunkerMovementType.OPENING_BALANCE,
                    balance,
                    balance,
                    BunkerReferenceType.INITIAL_SETUP,
                    saved.id(),
                    now,
                    actor.id(),
                    reason != null && !reason.trim().isBlank() ? reason.trim() : "Opening balance setup",
                    now
            ));

            return saved;
        });
    }

    @Override
    public List<BunkerStockMovement> listMovements(UUID tankId, int page, int size) {
        int limit = size > 0 ? size : 20;
        int offset = Math.max(0, page) * limit;
        return movements.findByTankIdPaged(tankId, offset, limit);
    }

    @Override
    public long countMovements(UUID tankId) {
        return movements.countByTankId(tankId);
    }

    @Override
    public DipReading recordDipReading(UUID tankId, BigDecimal physicalQuantityLiters, String notes, String actorName) {
        return transactions.execute(() -> {
            var actor = actor(actorName);
            var now = OffsetDateTime.now(clock);
            var tank = tanks.findById(tankId)
                    .orElseThrow(() -> new BusinessRuleException("BUNKER_TANK_NOT_FOUND", "Bunker tank not found: " + tankId));

            if (physicalQuantityLiters == null || physicalQuantityLiters.signum() < 0) {
                throw new BusinessRuleException("INVALID_BUNKER_QUANTITY", "Physical dip quantity must be non-negative");
            }

            BigDecimal physical = physicalQuantityLiters.setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP);
            BigDecimal book = tank.currentStockLiters();
            BigDecimal variance = physical.subtract(book).setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP);

            var reading = new DipReading(
                    UUID.randomUUID(),
                    tankId,
                    physical,
                    book,
                    variance,
                    now,
                    actor.id(),
                    notes != null ? notes.trim() : null,
                    now
            );

            return dipReadings.save(reading);
        });
    }

    @Override
    public List<DipReading> listDipReadings(UUID tankId) {
        return dipReadings.findByTankId(tankId);
    }

    @Override
    public StockAdjustment adjustStock(UUID tankId, BigDecimal quantityDeltaLiters, String reason, UUID sourceDipReadingId, String actorName) {
        return transactions.execute(() -> {
            var actor = actor(actorName);
            var now = OffsetDateTime.now(clock);

            if (reason == null || reason.trim().isBlank()) {
                throw new BusinessRuleException("INVALID_STOCK_ADJUSTMENT", "Adjustment reason is required");
            }
            if (quantityDeltaLiters == null || quantityDeltaLiters.signum() == 0) {
                throw new BusinessRuleException("INVALID_STOCK_ADJUSTMENT", "Adjustment delta cannot be zero");
            }

            var tank = tanks.findByIdForUpdate(tankId)
                    .orElseThrow(() -> new BusinessRuleException("BUNKER_TANK_NOT_FOUND", "Bunker tank not found: " + tankId));

            policy.requireActive(tank);

            BigDecimal delta = quantityDeltaLiters.setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP);
            BigDecimal resultingBalance = tank.currentStockLiters().add(delta).setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP);

            if (resultingBalance.signum() < 0) {
                throw new BusinessRuleException("INSUFFICIENT_BUNKER_STOCK",
                        "Adjustment would result in negative stock: " + resultingBalance + " L");
            }
            if (resultingBalance.compareTo(tank.capacityLiters()) > 0) {
                throw new BusinessRuleException("BUNKER_CAPACITY_EXCEEDED",
                        "Adjustment would exceed tank capacity: " + resultingBalance + " L > " + tank.capacityLiters() + " L");
            }

            var updatedTank = new BunkerTank(
                    tank.id(),
                    tank.fuelStationId(),
                    tank.tankCode(),
                    tank.tankName(),
                    tank.fuelType(),
                    tank.capacityLiters(),
                    resultingBalance,
                    tank.minimumStockLiters(),
                    tank.status(),
                    tank.commissionedAt(),
                    tank.active(),
                    tank.createdAt(),
                    now
            );
            tanks.save(updatedTank);

            BunkerMovementType movementType = delta.signum() > 0 ? BunkerMovementType.ADJUSTMENT_IN : BunkerMovementType.ADJUSTMENT_OUT;
            BigDecimal movementQty = delta.abs();

            var adjustment = new StockAdjustment(
                    UUID.randomUUID(),
                    tankId,
                    delta,
                    reason.trim(),
                    actor.id(),
                    sourceDipReadingId,
                    now,
                    now
            );
            var savedAdj = adjustments.save(adjustment);

            movements.save(new BunkerStockMovement(
                    UUID.randomUUID(),
                    tankId,
                    movementType,
                    movementQty,
                    resultingBalance,
                    BunkerReferenceType.MANUAL_ADJUSTMENT,
                    savedAdj.id(),
                    now,
                    actor.id(),
                    reason.trim(),
                    now
            ));

            return savedAdj;
        });
    }

    @Override
    public void transfer(TransferCommand command, String actorName) {
        transactions.execute(() -> {
            var actor = actor(actorName);
            var now = OffsetDateTime.now(clock);

            if (command.sourceTankId() == null || command.destinationTankId() == null) {
                throw new BusinessRuleException("INVALID_BUNKER_TRANSFER", "Source and destination tank IDs are required");
            }
            if (command.sourceTankId().equals(command.destinationTankId())) {
                throw new BusinessRuleException("INVALID_BUNKER_TRANSFER", "Source and destination tanks cannot be the same");
            }

            // Lock source and destination in consistent order (by UUID) to prevent deadlocks
            UUID firstId = command.sourceTankId().compareTo(command.destinationTankId()) < 0 ? command.sourceTankId() : command.destinationTankId();
            UUID secondId = firstId.equals(command.sourceTankId()) ? command.destinationTankId() : command.sourceTankId();

            var firstLocked = tanks.findByIdForUpdate(firstId)
                    .orElseThrow(() -> new BusinessRuleException("BUNKER_TANK_NOT_FOUND", "Bunker tank not found: " + firstId));
            var secondLocked = tanks.findByIdForUpdate(secondId)
                    .orElseThrow(() -> new BusinessRuleException("BUNKER_TANK_NOT_FOUND", "Bunker tank not found: " + secondId));

            var source = command.sourceTankId().equals(firstId) ? firstLocked : secondLocked;
            var destination = command.destinationTankId().equals(firstId) ? firstLocked : secondLocked;

            policy.validateTransfer(source, destination, command.quantityLiters());

            BigDecimal transferQty = command.quantityLiters().setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP);
            BigDecimal sourceNewBalance = source.currentStockLiters().subtract(transferQty).setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP);
            BigDecimal destNewBalance = destination.currentStockLiters().add(transferQty).setScale(BunkerTankPolicy.QUANTITY_SCALE, RoundingMode.HALF_UP);

            var updatedSource = new BunkerTank(
                    source.id(), source.fuelStationId(), source.tankCode(), source.tankName(), source.fuelType(),
                    source.capacityLiters(), sourceNewBalance, source.minimumStockLiters(), source.status(),
                    source.commissionedAt(), source.active(), source.createdAt(), now
            );
            var updatedDest = new BunkerTank(
                    destination.id(), destination.fuelStationId(), destination.tankCode(), destination.tankName(), destination.fuelType(),
                    destination.capacityLiters(), destNewBalance, destination.minimumStockLiters(), destination.status(),
                    destination.commissionedAt(), destination.active(), destination.createdAt(), now
            );

            tanks.save(updatedSource);
            tanks.save(updatedDest);

            UUID transferId = UUID.randomUUID();
            String reason = command.reason() != null && !command.reason().trim().isBlank() ? command.reason().trim() : "Tank transfer";

            movements.save(new BunkerStockMovement(
                    UUID.randomUUID(),
                    source.id(),
                    BunkerMovementType.TRANSFER_OUT,
                    transferQty,
                    sourceNewBalance,
                    BunkerReferenceType.TANK_TRANSFER,
                    transferId,
                    now,
                    actor.id(),
                    "Transfer to " + destination.tankCode() + ": " + reason,
                    now
            ));

            movements.save(new BunkerStockMovement(
                    UUID.randomUUID(),
                    destination.id(),
                    BunkerMovementType.TRANSFER_IN,
                    transferQty,
                    destNewBalance,
                    BunkerReferenceType.TANK_TRANSFER,
                    transferId,
                    now,
                    actor.id(),
                    "Transfer from " + source.tankCode() + ": " + reason,
                    now
            ));

            return null;
        });
    }
}
