-- US-73 governed outbound JSON file integration platform.

CREATE TABLE integration_configuration (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    normalized_name VARCHAR(160) NOT NULL,
    integration_type VARCHAR(32) NOT NULL,
    protocol VARCHAR(32) NOT NULL,
    direction VARCHAR(24) NOT NULL,
    endpoint_alias VARCHAR(80) NOT NULL,
    credential_reference VARCHAR(160),
    current_mapping_id UUID,
    data_classification VARCHAR(64) NOT NULL,
    retry_policy VARCHAR(40) NOT NULL,
    lifecycle VARCHAR(24) NOT NULL,
    health VARCHAR(24) NOT NULL,
    last_tested_at TIMESTAMPTZ,
    last_tested_version BIGINT,
    last_successful_exchange_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_integration_configuration_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_integration_configuration_name UNIQUE (tenant_id, normalized_name),
    CONSTRAINT ck_integration_configuration_type CHECK (integration_type = 'FILE_EXCHANGE'),
    CONSTRAINT ck_integration_configuration_protocol CHECK (protocol = 'FILE_JSON_V1'),
    CONSTRAINT ck_integration_configuration_direction CHECK (direction = 'OUTBOUND'),
    CONSTRAINT ck_integration_configuration_classification
        CHECK (data_classification = 'INTERNAL_OPERATIONAL_NON_SENSITIVE'),
    CONSTRAINT ck_integration_configuration_retry CHECK (retry_policy = 'US73_BOUNDED_V1'),
    CONSTRAINT ck_integration_configuration_lifecycle CHECK (lifecycle IN ('DRAFT', 'ACTIVE', 'DISABLED')),
    CONSTRAINT ck_integration_configuration_health
        CHECK (health IN ('UNKNOWN', 'HEALTHY', 'DEGRADED', 'UNAVAILABLE', 'AUTH_FAILED'))
);

CREATE INDEX idx_integration_configuration_tenant_lifecycle
    ON integration_configuration (tenant_id, lifecycle, normalized_name);

CREATE TABLE integration_mapping (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    configuration_id UUID NOT NULL,
    mapping_key VARCHAR(80) NOT NULL,
    mapping_version INTEGER NOT NULL,
    source_contract VARCHAR(100) NOT NULL,
    source_version INTEGER NOT NULL,
    target_schema VARCHAR(100) NOT NULL,
    target_version INTEGER NOT NULL,
    rules JSONB NOT NULL,
    definition_hash VARCHAR(64) NOT NULL,
    lifecycle VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_integration_mapping_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_integration_mapping_version
        UNIQUE (tenant_id, configuration_id, mapping_key, mapping_version),
    CONSTRAINT ck_integration_mapping_versions CHECK (mapping_version > 0 AND source_version > 0 AND target_version > 0),
    CONSTRAINT ck_integration_mapping_hash CHECK (definition_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_integration_mapping_lifecycle CHECK (lifecycle IN ('ACTIVE', 'SUPERSEDED')),
    CONSTRAINT fk_integration_mapping_configuration_tenant FOREIGN KEY (configuration_id, tenant_id)
        REFERENCES integration_configuration (id, tenant_id) ON DELETE RESTRICT
);

CREATE INDEX idx_integration_mapping_tenant_configuration
    ON integration_mapping (tenant_id, configuration_id, mapping_version DESC);

ALTER TABLE integration_configuration
    ADD CONSTRAINT fk_integration_configuration_mapping_tenant
    FOREIGN KEY (current_mapping_id, tenant_id)
    REFERENCES integration_mapping (id, tenant_id) ON DELETE RESTRICT;

CREATE TABLE integration_exchange (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    configuration_id UUID NOT NULL,
    source_event_id UUID NOT NULL,
    source_event_type VARCHAR(100) NOT NULL,
    mapping_version_id UUID NOT NULL,
    mapping_definition_hash VARCHAR(64) NOT NULL,
    canonical_payload JSONB NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    locked_until TIMESTAMPTZ,
    external_correlation_id VARCHAR(160),
    target_filename VARCHAR(80),
    last_error_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_integration_exchange_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_integration_exchange_idempotency
        UNIQUE (tenant_id, configuration_id, source_event_id, mapping_version_id),
    CONSTRAINT ck_integration_exchange_mapping_hash CHECK (mapping_definition_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_integration_exchange_payload_hash CHECK (payload_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_integration_exchange_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RETRY_SCHEDULED', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_integration_exchange_attempt_count CHECK (attempt_count BETWEEN 0 AND 5),
    CONSTRAINT fk_integration_exchange_configuration_tenant FOREIGN KEY (configuration_id, tenant_id)
        REFERENCES integration_configuration (id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_integration_exchange_mapping_tenant FOREIGN KEY (mapping_version_id, tenant_id)
        REFERENCES integration_mapping (id, tenant_id) ON DELETE RESTRICT
);

CREATE INDEX idx_integration_exchange_due
    ON integration_exchange (tenant_id, status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'RETRY_SCHEDULED');
CREATE INDEX idx_integration_exchange_stale_claim
    ON integration_exchange (tenant_id, status, locked_until)
    WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_integration_exchange_history
    ON integration_exchange (tenant_id, configuration_id, created_at DESC, id);

CREATE TABLE integration_exchange_attempt (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    exchange_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    latency_ms BIGINT NOT NULL,
    outcome VARCHAR(24) NOT NULL,
    error_code VARCHAR(80),
    external_correlation_id VARCHAR(160),
    target_filename VARCHAR(80),
    CONSTRAINT uq_integration_attempt_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_integration_attempt_number UNIQUE (tenant_id, exchange_id, attempt_number),
    CONSTRAINT ck_integration_attempt_number CHECK (attempt_number BETWEEN 1 AND 5),
    CONSTRAINT ck_integration_attempt_latency CHECK (latency_ms >= 0),
    CONSTRAINT ck_integration_attempt_outcome CHECK (outcome IN ('SUCCEEDED', 'RETRYABLE_FAILURE', 'PERMANENT_FAILURE')),
    CONSTRAINT fk_integration_attempt_exchange_tenant FOREIGN KEY (exchange_id, tenant_id)
        REFERENCES integration_exchange (id, tenant_id) ON DELETE RESTRICT
);

CREATE INDEX idx_integration_attempt_history
    ON integration_exchange_attempt (tenant_id, exchange_id, attempt_number);

CREATE TABLE integration_audit_event (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id UUID NOT NULL,
    outcome VARCHAR(24) NOT NULL,
    safe_code VARCHAR(80),
    before_hash VARCHAR(64),
    after_hash VARCHAR(64),
    correlation_id VARCHAR(160),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_integration_audit_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED')),
    CONSTRAINT ck_integration_audit_before_hash CHECK (before_hash IS NULL OR before_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_integration_audit_after_hash CHECK (after_hash IS NULL OR after_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX idx_integration_audit_history
    ON integration_audit_event (tenant_id, target_type, target_id, occurred_at DESC);

INSERT INTO app_permission (code, description, active) VALUES
    ('INTEGRATION_VIEW', 'View tenant-scoped integration configuration and health', TRUE),
    ('INTEGRATION_MANAGE', 'Create and update draft or disabled integration configurations', TRUE),
    ('INTEGRATION_TEST', 'Test tenant-scoped integration endpoint connectivity', TRUE),
    ('INTEGRATION_ACTIVATE', 'Enable and disable tenant-scoped integration configurations', TRUE),
    ('INTEGRATION_AUDIT_VIEW', 'View tenant-scoped integration exchange and attempt history', TRUE),
    ('INTEGRATION_RECONCILE', 'Reserved external integration reconciliation permission', TRUE)
ON CONFLICT (code) DO UPDATE SET description = EXCLUDED.description, active = TRUE;
