package com.transportlogistics.app.freight.insurance.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FreightInsurancePolicyJpaRepository extends JpaRepository<FreightInsurancePolicyEntity, UUID> {

    Optional<FreightInsurancePolicyEntity> findByFreightOrderId(UUID freightOrderId);
}
