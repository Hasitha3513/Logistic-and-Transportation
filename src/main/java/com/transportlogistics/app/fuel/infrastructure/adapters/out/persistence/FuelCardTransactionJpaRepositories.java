package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;
import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List; import java.util.Optional; import java.util.UUID;
interface FuelCardImportBatchJpaRepository extends JpaRepository<FuelCardImportBatchJpaEntity,UUID>{
 Optional<FuelCardImportBatchJpaEntity> findByTenantIdAndProviderIdAndProviderBatchId(UUID t,UUID p,String id);
 Optional<FuelCardImportBatchJpaEntity> findByTenantIdAndProviderIdAndFileHash(UUID t,UUID p,String hash);
 Optional<FuelCardImportBatchJpaEntity> findByTenantIdAndId(UUID t,UUID id);
 List<FuelCardImportBatchJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID t,Pageable p);
}
interface FuelCardTransactionJpaRepository extends JpaRepository<FuelCardTransactionJpaEntity,UUID>,JpaSpecificationExecutor<FuelCardTransactionJpaEntity>{
 Optional<FuelCardTransactionJpaEntity> findByTenantIdAndProviderIdAndProviderTransactionId(UUID t,UUID p,String id);
 Optional<FuelCardTransactionJpaEntity> findByTenantIdAndId(UUID t,UUID id);
 List<FuelCardTransactionJpaEntity> findByTenantIdOrderByTransactionTimestampDesc(UUID t,Pageable p);
 @Query("select coalesce(sum(x.totalAmount),0), coalesce(sum(x.quantityLitres),0) from FuelCardTransactionJpaEntity x "
         + "where x.tenantId=:tenantId and x.cardId=:cardId and x.transactionKind='PURCHASE' "
         + "and x.transactionTimestamp>=:from and x.transactionTimestamp<:to")
 Object[] totals(@Param("tenantId") UUID tenantId, @Param("cardId") UUID cardId,
                 @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
interface FuelCardIndicatorJpaRepository extends JpaRepository<FuelCardIndicatorJpaEntity,UUID>{
 List<FuelCardIndicatorJpaEntity> findByTenantIdAndTransactionId(UUID t,UUID id);
}
interface FuelCardReconciliationJpaRepository extends JpaRepository<FuelCardReconciliationJpaEntity,UUID>{}
