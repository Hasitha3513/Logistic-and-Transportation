package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.FuelCard;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="fuel_card") @Getter @Setter @NoArgsConstructor
class FuelCardJpaEntity extends TenantScopedEntity {
    @Id UUID id; @Column(nullable=false) UUID providerId; @Column(nullable=false,length=100) String alias;
    @Column(nullable=false,length=255) String providerCardReference;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable=false,columnDefinition="char(64)") String providerReferenceHash;
    @Column(nullable=false,length=32) String maskedIdentifier;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(columnDefinition="char(4)") String lastFour;
    @Column(nullable=false) short expiryMonth; @Column(nullable=false) short expiryYear;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) FuelCard.Status status;
    @Column(nullable=false,length=20) String providerSyncStatus = "NOT_CONFIGURED";
    @Version long version; @Column(nullable=false) UUID createdBy;
    @Column(nullable=false) OffsetDateTime createdAt; @Column(nullable=false) OffsetDateTime updatedAt;
}

@Entity @Table(name="fuel_card_binding_history") @Getter @Setter @NoArgsConstructor
class FuelCardBindingJpaEntity extends TenantScopedEntity {
    @Id UUID id; @Column(nullable=false) UUID cardId; @Column(nullable=false,length=10) String bindingType;
    @Column(nullable=false) UUID bindingId; @Column(nullable=false) OffsetDateTime effectiveFrom;
    OffsetDateTime effectiveTo; @Column(nullable=false,length=500) String reason;
    @Column(nullable=false) UUID changedBy; @Column(nullable=false) OffsetDateTime createdAt;
}

@Entity @Table(name="fuel_card_restriction") @Getter @Setter @NoArgsConstructor
class FuelCardRestrictionJpaEntity extends TenantScopedEntity {
    @Id UUID id; @Column(nullable=false) UUID cardId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable=false,columnDefinition="char(3)") String currency;
    @Column(nullable=false,precision=19,scale=2) BigDecimal maxTransactionAmount;
    @Column(nullable=false,precision=19,scale=2) BigDecimal maxDailyAmount;
    @Column(nullable=false,precision=19,scale=2) BigDecimal maxMonthlyAmount;
    @Column(nullable=false,precision=19,scale=4) BigDecimal maxDailyLitres;
    @Column(nullable=false,columnDefinition="text") String allowedFuelTypes;
    @Column(nullable=false,columnDefinition="text") String allowedStationReferences;
    @Version long version; @Column(nullable=false) UUID changedBy; @Column(nullable=false) OffsetDateTime changedAt;
}

@Entity @Table(name="fuel_card_audit_event") @Getter @Setter @NoArgsConstructor
class FuelCardAuditJpaEntity extends TenantScopedEntity {
    @Id UUID id; UUID cardId; UUID transactionId; @Column(nullable=false,length=50) String action;
    @Column(nullable=false,length=30) String result; @Column(length=80) String reasonCode;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(columnDefinition="char(64)") String beforeHash;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(columnDefinition="char(64)") String afterHash;
    @Column(nullable=false) UUID actorId; @Column(nullable=false) OffsetDateTime createdAt;
}
