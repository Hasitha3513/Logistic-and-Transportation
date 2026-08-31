-- V48__delivery_failed_attempts_us59.sql
-- US-59 Manage Failed Deliveries: Lifecycle extension, DeliveryAttempt, ContactAttempt, Escalation tables & RBAC

-- 1. Update delivery_order status check constraint to include FAILED_ATTEMPT, RETURN_TO_BASE, ESCALATED
ALTER TABLE delivery_order DROP CONSTRAINT IF EXISTS ck_delivery_order_status;
ALTER TABLE delivery_order DROP CONSTRAINT IF EXISTS delivery_order_status_check;
ALTER TABLE delivery_order ADD CONSTRAINT ck_delivery_order_status
    CHECK (status IN ('DRAFT', 'READY_FOR_ASSIGNMENT', 'DELIVERED', 'FAILED_ATTEMPT', 'RETURN_TO_BASE', 'ESCALATED'));

-- 2. Create delivery_attempt table
CREATE TABLE IF NOT EXISTS delivery_attempt (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_id UUID NOT NULL,
    attempt_number INT NOT NULL,
    attempt_timestamp TIMESTAMPTZ NOT NULL,
    failure_reason VARCHAR(50) NOT NULL,
    notes TEXT,
    disposition VARCHAR(50) NOT NULL,
    recorded_by VARCHAR(128) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_delivery_attempt_delivery FOREIGN KEY (delivery_id) REFERENCES delivery_order(id) ON DELETE CASCADE,
    CONSTRAINT uq_delivery_attempt_number UNIQUE (tenant_id, delivery_id, attempt_number)
);

CREATE INDEX IF NOT EXISTS idx_delivery_attempt_lookup ON delivery_attempt(tenant_id, delivery_id);
CREATE INDEX IF NOT EXISTS idx_delivery_attempt_time ON delivery_attempt(tenant_id, attempt_timestamp);

-- 3. Create delivery_contact_attempt table
CREATE TABLE IF NOT EXISTS delivery_contact_attempt (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_attempt_id UUID NOT NULL,
    channel VARCHAR(50) NOT NULL,
    contact_timestamp TIMESTAMPTZ NOT NULL,
    outcome VARCHAR(50) NOT NULL,
    notes VARCHAR(500),
    recorded_by VARCHAR(128) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_delivery_contact_attempt FOREIGN KEY (delivery_attempt_id) REFERENCES delivery_attempt(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_delivery_contact_attempt_lookup ON delivery_contact_attempt(tenant_id, delivery_attempt_id);

-- 4. Create delivery_escalation table
CREATE TABLE IF NOT EXISTS delivery_escalation (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_id UUID NOT NULL,
    delivery_attempt_id UUID,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    resolution_notes TEXT,
    escalated_by VARCHAR(128) NOT NULL,
    escalated_at TIMESTAMPTZ NOT NULL,
    resolved_by VARCHAR(128),
    resolved_at TIMESTAMPTZ,
    CONSTRAINT fk_delivery_escalation_delivery FOREIGN KEY (delivery_id) REFERENCES delivery_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_escalation_attempt FOREIGN KEY (delivery_attempt_id) REFERENCES delivery_attempt(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_delivery_escalation_lookup ON delivery_escalation(tenant_id, delivery_id);
CREATE INDEX IF NOT EXISTS idx_delivery_escalation_status ON delivery_escalation(tenant_id, status);

-- 5. Seed US-59 Permissions
INSERT INTO app_permission (code, description, active) VALUES
    ('DELIVERY_FAIL_RECORD', 'Record failed delivery attempt, contact attempt, and disposition', TRUE),
    ('DELIVERY_FAIL_VIEW', 'View failed delivery history, contact attempts, and escalation records', TRUE),
    ('DELIVERY_FAIL_ESCALATE', 'Escalate failed delivery and update escalation records', TRUE),
    ('DELIVERY_RETURN_INITIATE', 'Initiate Return-to-Base disposition for delivery order', TRUE);
