package com.transportlogistics.app.integration.adapters.outbound.persistence;

import com.transportlogistics.app.integration.domain.model.IntegrationMapping;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_mapping")
@Getter
@Setter
class IntegrationMappingEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "configuration_id", nullable = false, updatable = false) private UUID configurationId;
    @Column(name = "mapping_key", nullable = false, updatable = false, length = 80) private String mappingKey;
    @Column(name = "mapping_version", nullable = false, updatable = false) private int mappingVersion;
    @Column(name = "source_contract", nullable = false, updatable = false, length = 100) private String sourceContract;
    @Column(name = "source_version", nullable = false, updatable = false) private int sourceVersion;
    @Column(name = "target_schema", nullable = false, updatable = false, length = 100) private String targetSchema;
    @Column(name = "target_version", nullable = false, updatable = false) private int targetVersion;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb") private String rules;
    @Column(name = "definition_hash", nullable = false, updatable = false, length = 64) private String definitionHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private IntegrationMapping.Lifecycle lifecycle;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "created_by", nullable = false, updatable = false, length = 255) private String createdBy;
}
