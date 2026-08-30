package com.transportlogistics.app.freight.manifest.adapters.inbound.web.mappers;

import com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.request.CargoManifestItemRequest;
import com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.response.CargoManifestItemResponse;
import com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.response.CargoManifestReadinessResponse;
import com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.response.CargoManifestResponse;
import com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.response.ManifestValidationFailureResponse;
import com.transportlogistics.app.freight.manifest.domain.model.CargoManifest;
import com.transportlogistics.app.freight.manifest.ports.inbound.CargoManifestUseCase;
import org.springframework.stereotype.Component;

@Component
public class CargoManifestWebMapper {

    public CargoManifestUseCase.ItemCommand toCommand(CargoManifestItemRequest r) {
        return new CargoManifestUseCase.ItemCommand(
                r.version(),
                r.freightOrderLineId(),
                r.description(),
                r.quantity(),
                r.packingInformation(),
                r.commodityClassification(),
                r.customsApplicable(),
                r.customsInformation(),
                r.hazardous(),
                r.hazardousClassification(),
                r.hazardousDetails(),
                r.fragile(),
                r.temperatureSensitive(),
                r.unitWeight(),
                r.weightUnit(),
                r.length(),
                r.width(),
                r.height(),
                r.dimensionUnit()
        );
    }

    public CargoManifestResponse toResponse(CargoManifest m) {
        return new CargoManifestResponse(
                m.id(),
                m.manifestNumber(),
                m.freightOrderId(),
                m.freightOrderNumber(),
                m.finalized(),
                m.items().stream().map(i -> new CargoManifestItemResponse(
                        i.id(),
                        i.freightOrderLineId(),
                        i.description(),
                        i.quantity(),
                        i.packingInformation(),
                        i.commodityClassification(),
                        i.customsApplicable(),
                        i.customsInformation(),
                        i.hazardous(),
                        i.hazardousClassification(),
                        i.hazardousDetails(),
                        i.fragile(),
                        i.temperatureSensitive(),
                        i.unitWeight(),
                        i.weightUnit(),
                        i.length(),
                        i.width(),
                        i.height(),
                        i.dimensionUnit()
                )).toList(),
                m.version(),
                m.createdAt(),
                m.updatedAt(),
                m.createdBy(),
                m.updatedBy(),
                m.finalizedAt(),
                m.finalizedBy()
        );
    }

    public CargoManifestReadinessResponse toResponse(CargoManifestUseCase.Readiness r) {
        return new CargoManifestReadinessResponse(
                r.ready(),
                r.failures().stream().map(f -> new ManifestValidationFailureResponse(f.code(), f.field(), f.message())).toList()
        );
    }
}
