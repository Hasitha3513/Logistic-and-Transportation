package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.math.BigDecimal; import java.time.OffsetDateTime; import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes;

@Entity @Table(name="fuel_card_import_batch") @Getter @Setter @NoArgsConstructor
class FuelCardImportBatchJpaEntity extends TenantScopedEntity {
 @Id UUID id; @Column(nullable=false) UUID providerId; @Column(nullable=false,length=120) String providerBatchId;
 @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable=false,columnDefinition="char(64)") String fileHash;
 @Column(nullable=false) OffsetDateTime generatedAt;
 @Column(nullable=false) int transactionCount; @Column(nullable=false) int importedCount; @Column(nullable=false) int reviewCount;
 @Column(nullable=false) UUID importedBy; @Column(nullable=false) OffsetDateTime createdAt;
}
@Entity @Table(name="fuel_card_transaction") @Getter @Setter @NoArgsConstructor
class FuelCardTransactionJpaEntity extends TenantScopedEntity {
 @Id UUID id; @Column(nullable=false) UUID batchId; @Column(nullable=false) UUID providerId; @Column(nullable=false) UUID cardId;
 @Column(nullable=false,length=120) String providerTransactionId;
 @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable=false,columnDefinition="char(64)") String canonicalHash;
 @Column(nullable=false,length=10) String transactionKind; @Column(length=120) String originalProviderTransactionId;
 @Column(nullable=false) OffsetDateTime transactionTimestamp; OffsetDateTime postedTimestamp; @Column(length=120) String stationReference;
 @Column(nullable=false,length=40) String fuelType; @Column(nullable=false,precision=19,scale=4) BigDecimal quantityLitres;
 @Column(nullable=false,precision=19,scale=4) BigDecimal unitPrice; @Column(nullable=false,precision=19,scale=2) BigDecimal totalAmount;
 @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable=false,columnDefinition="char(3)") String currency;
 @Column(length=120) String providerVehicleReference;
 @Column(length=120) String providerDriverReference; UUID tripId; @Column(nullable=false,length=10) String providerStatus;
 @Column(nullable=false,length=20) String localStatus; UUID reconciledPurchaseId; @Column(nullable=false) UUID importedBy;
 @Version long version; @Column(nullable=false) OffsetDateTime createdAt;
}
@Entity @Table(name="fuel_card_transaction_indicator") @Getter @Setter @NoArgsConstructor
class FuelCardIndicatorJpaEntity extends TenantScopedEntity {
 @Id UUID id; @Column(nullable=false) UUID transactionId; @Column(nullable=false,length=40) String code;
 @Column(length=60) String detailCode; @Column(nullable=false) OffsetDateTime createdAt; UUID acknowledgedBy; OffsetDateTime acknowledgedAt;
}
@Entity @Table(name="fuel_card_reconciliation_history") @Getter @Setter @NoArgsConstructor
class FuelCardReconciliationJpaEntity extends TenantScopedEntity {
 @Id UUID id; @Column(nullable=false) UUID transactionId; @Column(nullable=false,length=24) String action;
 UUID purchaseId; @Column(nullable=false,length=500) String reason; @Column(nullable=false) UUID actorId;
 @Column(nullable=false) OffsetDateTime createdAt;
}
