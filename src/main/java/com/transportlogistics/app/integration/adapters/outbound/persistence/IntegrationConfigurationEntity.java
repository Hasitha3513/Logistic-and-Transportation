package com.transportlogistics.app.integration.adapters.outbound.persistence;

import com.transportlogistics.app.integration.domain.model.IntegrationConfiguration;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_configuration")
@Getter
@Setter
class IntegrationConfigurationEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 160) private String name;
    @Column(name = "normalized_name", nullable = false, length = 160) private String normalizedName;
    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 32)
    private IntegrationConfiguration.Type type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private IntegrationConfiguration.Protocol protocol;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private IntegrationConfiguration.Direction direction;
    @Column(name = "endpoint_alias", nullable = false, length = 80) private String endpointAlias;
    @Column(name = "credential_reference", length = 160) private String credentialReference;
    @Column(name = "current_mapping_id") private UUID currentMappingId;
    @Enumerated(EnumType.STRING)
    @Column(name = "data_classification", nullable = false, length = 64)
    private IntegrationConfiguration.DataClassification dataClassification;
    @Enumerated(EnumType.STRING) @Column(name = "retry_policy", nullable = false, length = 40)
    private IntegrationConfiguration.RetryPolicy retryPolicy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private IntegrationConfiguration.Lifecycle lifecycle;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private IntegrationConfiguration.Health health;
    @Column(name = "last_tested_at") private OffsetDateTime lastTestedAt;
    @Column(name = "last_tested_version") private Long lastTestedVersion;
    @Column(name = "last_successful_exchange_at") private OffsetDateTime lastSuccessfulExchangeAt;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "created_by", nullable = false, updatable = false, length = 255) private String createdBy;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "updated_by", nullable = false, length = 255) private String updatedBy;
}
