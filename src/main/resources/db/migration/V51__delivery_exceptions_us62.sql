-- ============================================================================
-- Migration: V51__delivery_exceptions_us62.sql
-- Module: Delivery (MVP 1.3 - US-62 Handle Delivery Exceptions)
-- ============================================================================

-- 1. Ensure composite unique constraints for composite same-tenant FKs
ALTER TABLE delivery_attempt ADD CONSTRAINT uk_delivery_attempt_id_tenant UNIQUE (id, tenant_id);

-- 2. Table: delivery_exception_case
CREATE TABLE delivery_exception_case (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_order_id UUID NOT NULL,
    delivery_attempt_id UUID,
    exception_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    description VARCHAR(1000) NOT NULL,
    corrected_location_id UUID,
    otp_attempt_reference VARCHAR(100),
    delivered_items_description VARCHAR(1000),
    undelivered_items_description VARCHAR(1000),
    quantity_delivered NUMERIC(12,2),
    quantity_undelivered NUMERIC(12,2),
    resolution_code VARCHAR(50),
    resolution_notes VARCHAR(1000),
    follow_up_disposition VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    reported_at TIMESTAMPTZ NOT NULL,
    reported_by VARCHAR(128) NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolved_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_delivery_exception_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_exc_delivery_tenant FOREIGN KEY (delivery_order_id, tenant_id)
        REFERENCES delivery_order(id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_exc_attempt_tenant FOREIGN KEY (delivery_attempt_id, tenant_id)
        REFERENCES delivery_attempt(id, tenant_id) ON DELETE SET NULL,
    CONSTRAINT chk_exc_type CHECK (exception_type IN (
        'DAMAGED_DELIVERY', 'WRONG_ADDRESS', 'PARTIAL_DELIVERY', 'OTP_MISMATCH', 'RECIPIENT_REFUSAL'
    )),
    CONSTRAINT chk_exc_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_exc_status CHECK (status IN ('OPEN', 'UNDER_INVESTIGATION', 'RESOLVED', 'CANCELLED')),
    CONSTRAINT chk_exc_resolution CHECK (
        (status IN ('OPEN', 'UNDER_INVESTIGATION', 'CANCELLED') AND resolution_code IS NULL AND resolved_at IS NULL) OR
        (status = 'RESOLVED' AND resolution_code IS NOT NULL AND resolved_at IS NOT NULL AND resolved_by IS NOT NULL)
    )
);

-- 3. Table: delivery_exception_evidence
CREATE TABLE delivery_exception_evidence (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    exception_case_id UUID NOT NULL,
    storage_reference VARCHAR(255) NOT NULL,
    detected_content_type VARCHAR(50) NOT NULL,
    content_length BIGINT NOT NULL,
    sha256_checksum VARCHAR(64) NOT NULL,
    original_filename VARCHAR(255),
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exc_evidence_case_tenant FOREIGN KEY (exception_case_id, tenant_id)
        REFERENCES delivery_exception_case(id, tenant_id) ON DELETE CASCADE
);

-- 4. Indexes
CREATE INDEX idx_exc_tenant_delivery ON delivery_exception_case(tenant_id, delivery_order_id);
CREATE INDEX idx_exc_tenant_status ON delivery_exception_case(tenant_id, status);
CREATE INDEX idx_exc_tenant_type ON delivery_exception_case(tenant_id, exception_type);
CREATE INDEX idx_exc_tenant_reported ON delivery_exception_case(tenant_id, reported_at DESC);
CREATE INDEX idx_exc_evidence_case ON delivery_exception_evidence(tenant_id, exception_case_id);

-- Partial Unique Index to prevent duplicate active exception of same type on same delivery order
CREATE UNIQUE INDEX uk_active_delivery_exception_type 
ON delivery_exception_case(tenant_id, delivery_order_id, exception_type) 
WHERE status IN ('OPEN', 'UNDER_INVESTIGATION');

-- 5. Seed US-62 Permissions
INSERT INTO app_permission (code, description, active) VALUES
    ('DELIVERY_EXCEPTION_CREATE', 'Report delivery exception cases', TRUE),
    ('DELIVERY_EXCEPTION_VIEW', 'View delivery exception cases and history', TRUE),
    ('DELIVERY_EXCEPTION_MANAGE', 'Investigate and update delivery exception cases', TRUE),
    ('DELIVERY_EXCEPTION_RESOLVE', 'Resolve or cancel delivery exception cases', TRUE),
    ('DELIVERY_EXCEPTION_ESCALATE', 'Escalate delivery exception cases', TRUE)
ON CONFLICT (code) DO UPDATE SET description = EXCLUDED.description, active = TRUE;
