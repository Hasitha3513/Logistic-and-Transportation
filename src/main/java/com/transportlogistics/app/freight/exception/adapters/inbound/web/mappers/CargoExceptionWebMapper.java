package com.transportlogistics.app.freight.exception.adapters.inbound.web.mappers;

import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.response.CargoExceptionHistoryResponse;
import com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.response.CargoExceptionResponse;
import com.transportlogistics.app.freight.exception.domain.CargoException;
import com.transportlogistics.app.freight.exception.domain.CargoExceptionHistoryEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CargoExceptionWebMapper {

    public CargoExceptionResponse toResponse(CargoException exception) {
        List<CargoExceptionHistoryResponse> historyResponses = exception.getHistory()
                .stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());

        return new CargoExceptionResponse(
                exception.getId(),
                exception.getExceptionNumber(),
                exception.getExceptionType().name(),
                exception.getStatus().name(),
                exception.getSeverity().name(),
                exception.getFreightOrderId(),
                exception.getManifestId(),
                exception.getManifestItemId(),
                exception.getDescription(),
                exception.getImpact(),
                exception.getRestriction(),
                exception.getCorrectiveAction(),
                exception.getResolution(),
                exception.getResolvedAt(),
                exception.getResolvedBy(),
                historyResponses,
                exception.getCreatedAt(),
                exception.getUpdatedAt(),
                exception.getCreatedBy(),
                exception.getUpdatedBy(),
                exception.getVersion()
        );
    }

    private CargoExceptionHistoryResponse toHistoryResponse(CargoExceptionHistoryEntry entry) {
        return new CargoExceptionHistoryResponse(
                entry.getId(),
                entry.getAction(),
                entry.getActor(),
                entry.getOccurredAt(),
                entry.getReason(),
                entry.getDetails()
        );
    }
}
