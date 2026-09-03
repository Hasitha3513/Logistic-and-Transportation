CREATE TABLE integration_outbox_event (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INTEGER NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    locked_until TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    last_error_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_integration_outbox_logical_delivery
        UNIQUE (tenant_id, event_id, consumer_name),
    CONSTRAINT ck_integration_outbox_version CHECK (event_version >= 1),
    CONSTRAINT ck_integration_outbox_attempt_count CHECK (attempt_count BETWEEN 0 AND 5),
    CONSTRAINT ck_integration_outbox_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'RETRY', 'PUBLISHED', 'FAILED', 'UNSUPPORTED')
    )
);

CREATE INDEX idx_integration_outbox_ready
    ON integration_outbox_event (tenant_id, status, next_attempt_at, occurred_at)
    WHERE status IN ('PENDING', 'RETRY');

CREATE INDEX idx_integration_outbox_stale_claim
    ON integration_outbox_event (tenant_id, status, locked_until)
    WHERE status = 'PROCESSING';

CREATE INDEX idx_integration_outbox_terminal
    ON integration_outbox_event (tenant_id, status, updated_at)
    WHERE status IN ('PUBLISHED', 'FAILED', 'UNSUPPORTED');

CREATE INDEX idx_integration_outbox_event_identity
    ON integration_outbox_event (tenant_id, event_id);
