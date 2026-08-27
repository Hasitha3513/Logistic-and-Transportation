package com.transportlogistics.app.freight.exception.adapters.outbound.persistence;

import com.transportlogistics.app.freight.exception.domain.CargoException;
import com.transportlogistics.app.freight.exception.domain.CargoExceptionHistoryEntry;
import com.transportlogistics.app.freight.exception.domain.ExceptionSeverity;
import com.transportlogistics.app.freight.exception.domain.ExceptionStatus;
import com.transportlogistics.app.freight.exception.domain.ExceptionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CargoExceptionPersistenceMapper {

    // ── entity → domain ───────────────────────────────────────────────────────

    public CargoException toDomain(CargoExceptionEntity entity) {
        List<CargoExceptionHistoryEntry> history = entity.getHistoryEntries().stream()
                .map(this::toHistoryDomain)
                .collect(Collectors.toList());

        return new CargoException(
                entity.getId(),
                entity.getExceptionNumber(),
                ExceptionType.valueOf(entity.getExceptionType()),
                ExceptionStatus.valueOf(entity.getStatus()),
                ExceptionSeverity.valueOf(entity.getSeverity()),
                entity.getFreightOrderId(),
                entity.getManifestId(),
                entity.getManifestItemId(),
                entity.getDescription(),
                entity.getImpact(),
                entity.getRestriction(),
                entity.getCorrectiveAction(),
                entity.getResolution(),
                entity.getResolvedAt(),
                entity.getResolvedBy(),
                history,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getVersion()
        );
    }

    private CargoExceptionHistoryEntry toHistoryDomain(CargoExceptionHistoryEntity h) {
        return new CargoExceptionHistoryEntry(
                h.getId(),
                h.getExceptionEntity().getId(),
                h.getAction(),
                h.getActor(),
                h.getOccurredAt(),
                h.getReason(),
                h.getDetails()
        );
    }

    // ── domain → entity (update in-place) ────────────────────────────────────

    public void updateEntity(CargoExceptionEntity entity, CargoException domain) {
        entity.setId(domain.getId());
        entity.setExceptionNumber(domain.getExceptionNumber());
        entity.setExceptionType(domain.getExceptionType().name());
        entity.setStatus(domain.getStatus().name());
        entity.setSeverity(domain.getSeverity().name());
        entity.setFreightOrderId(domain.getFreightOrderId());
        entity.setManifestId(domain.getManifestId());
        entity.setManifestItemId(domain.getManifestItemId());
        entity.setDescription(domain.getDescription());
        entity.setImpact(domain.getImpact());
        entity.setRestriction(domain.getRestriction());
        entity.setCorrectiveAction(domain.getCorrectiveAction());
        entity.setResolution(domain.getResolution());
        entity.setResolvedAt(domain.getResolvedAt());
        entity.setResolvedBy(domain.getResolvedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedBy(domain.getUpdatedBy());

        // Replace history entries (cascade ALL + orphanRemoval)
        List<CargoExceptionHistoryEntity> historyEntities = domain.getHistory().stream()
                .map(h -> toHistoryEntity(h))
                .collect(Collectors.toList());
        entity.replaceHistory(historyEntities);
    }

    private CargoExceptionHistoryEntity toHistoryEntity(CargoExceptionHistoryEntry h) {
        CargoExceptionHistoryEntity entity = new CargoExceptionHistoryEntity();
        entity.setId(h.getId());
        entity.setAction(h.getAction());
        entity.setActor(h.getActor());
        entity.setOccurredAt(h.getOccurredAt());
        entity.setReason(h.getReason());
        entity.setDetails(h.getDetails());
        return entity;
    }
}
