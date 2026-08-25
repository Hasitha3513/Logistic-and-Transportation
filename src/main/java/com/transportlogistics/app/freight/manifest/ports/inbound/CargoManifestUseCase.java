package com.transportlogistics.app.freight.manifest.ports.inbound;

import com.transportlogistics.app.freight.manifest.domain.model.CargoManifest;
import com.transportlogistics.app.freight.manifest.domain.model.ManifestValidationFailure;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CargoManifestUseCase {
    CargoManifest create(CreateCommand command, String actor);
    CargoManifest get(UUID id);
    PageResult<CargoManifest> search(SearchQuery query);
    CargoManifest update(UUID id, UpdateCommand command, String actor);
    CargoManifest addItem(UUID id, ItemCommand command, String actor);
    CargoManifest updateItem(UUID id, UUID itemId, ItemCommand command, String actor);
    Readiness validate(UUID id);
    CargoManifest finalizeManifest(UUID id, long version, String actor);
    record CreateCommand(UUID freightOrderId) { }
    record UpdateCommand(Long version) { }
    record ItemCommand(Long version, UUID freightOrderLineId, String description, BigDecimal quantity,
                       String packingInformation, String commodityClassification, boolean customsApplicable,
                       String customsInformation, boolean hazardous, String hazardousClassification, String hazardousDetails) { }
    record SearchQuery(String search, UUID freightOrderId, Boolean finalized, int page, int limit, String sort, String direction) { }
    record PageResult<T>(List<T> content,int page,int limit,long totalElements,int totalPages) { }
    record Readiness(boolean ready, List<ManifestValidationFailure> failures) { public Readiness { failures=List.copyOf(failures); } }
}
