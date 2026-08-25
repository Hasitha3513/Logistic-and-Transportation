package com.transportlogistics.app.freight.manifest.ports.outbound;
import com.transportlogistics.app.freight.manifest.domain.model.CargoManifest;
import com.transportlogistics.app.freight.manifest.ports.inbound.CargoManifestUseCase;
import java.util.Optional; import java.util.UUID;
public interface CargoManifestRepository { CargoManifest save(CargoManifest manifest); Optional<CargoManifest> findById(UUID id); CargoManifestUseCase.PageResult<CargoManifest> search(CargoManifestUseCase.SearchQuery query); }
