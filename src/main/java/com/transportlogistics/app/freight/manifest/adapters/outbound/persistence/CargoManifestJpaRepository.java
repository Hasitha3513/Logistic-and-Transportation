package com.transportlogistics.app.freight.manifest.adapters.outbound.persistence;
import org.springframework.data.jpa.repository.*; import java.util.UUID;
interface CargoManifestJpaRepository extends JpaRepository<CargoManifestEntity,UUID>, JpaSpecificationExecutor<CargoManifestEntity> { }
