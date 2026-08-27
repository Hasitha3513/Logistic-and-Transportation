package com.transportlogistics.app.freight.loadplanning.adapters.outbound.manifest;

import com.transportlogistics.app.freight.loadplanning.ports.inbound.CargoManifestLookupPort;
import com.transportlogistics.app.freight.manifest.domain.model.CargoManifest;
import com.transportlogistics.app.freight.manifest.domain.model.CargoManifestItem;
import com.transportlogistics.app.freight.manifest.ports.inbound.CargoManifestUseCase;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ManifestLoadPlanLookupAdapter implements CargoManifestLookupPort {

    private final CargoManifestUseCase cargoManifestUseCase;

    public ManifestLoadPlanLookupAdapter(CargoManifestUseCase cargoManifestUseCase) {
        this.cargoManifestUseCase = cargoManifestUseCase;
    }

    @Override
    public Optional<ManifestPlanningView> findManifest(UUID cargoManifestId) {
        try {
            CargoManifest manifest = cargoManifestUseCase.get(cargoManifestId);
            List<ManifestItemPlanningView> items = manifest.items().stream()
                    .map(this::toItemView)
                    .toList();

            return Optional.of(new ManifestPlanningView(
                    manifest.id(),
                    manifest.manifestNumber(),
                    manifest.finalized(),
                    items
            ));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    private ManifestItemPlanningView toItemView(CargoManifestItem item) {
        return new ManifestItemPlanningView(
                item.id(),
                item.description(),
                item.quantity(),
                item.packingInformation(),
                item.commodityClassification(),
                item.hazardous(),
                item.hazardousClassification(),
                item.fragile(),
                item.temperatureSensitive(),
                item.unitWeight(),
                item.weightUnit(),
                item.length(),
                item.width(),
                item.height(),
                item.dimensionUnit()
        );
    }
}
