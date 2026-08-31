-- ============================================================================
-- Migration: V49__delivery_redelivery_us60.sql
-- Module: Delivery (MVP 1.3 - US-60 Schedule Re-Delivery)
-- ============================================================================

-- 1. Table: delivery_redelivery_schedule
CREATE TABLE delivery_redelivery_schedule (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_order_id UUID NOT NULL,
    delivery_attempt_id UUID NOT NULL,
    scheduling_method VARCHAR(32) NOT NULL,
    preferred_start_time TIMESTAMP WITH TIME ZONE,
    preferred_end_time TIMESTAMP WITH TIME ZONE,
    customer_preference_notes VARCHAR(500),
    scheduled_start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduled_end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    scheduled_by VARCHAR(100) NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    superseded_at TIMESTAMP WITH TIME ZONE,
    superseded_by VARCHAR(100),
    supersede_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_redelivery_order FOREIGN KEY (delivery_order_id) REFERENCES delivery_order(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_redelivery_attempt FOREIGN KEY (delivery_attempt_id) REFERENCES delivery_attempt(id) ON DELETE RESTRICT,
    CONSTRAINT chk_delivery_redelivery_window CHECK (scheduled_start_time < scheduled_end_time),
    CONSTRAINT chk_delivery_redelivery_method CHECK (scheduling_method IN ('AUTOMATIC', 'AGENT_ASSISTED')),
    CONSTRAINT chk_delivery_redelivery_status CHECK (status IN ('CONFIRMED', 'SUPERSEDED', 'CANCELLED'))
);

-- 2. Indexes
CREATE INDEX idx_delivery_redelivery_tenant_delivery ON delivery_redelivery_schedule(tenant_id, delivery_order_id);
CREATE INDEX idx_delivery_redelivery_tenant_attempt ON delivery_redelivery_schedule(tenant_id, delivery_attempt_id);
CREATE INDEX idx_delivery_redelivery_tenant_status ON delivery_redelivery_schedule(tenant_id, status);
CREATE INDEX idx_delivery_redelivery_window ON delivery_redelivery_schedule(tenant_id, status, scheduled_start_time, scheduled_end_time);

-- 3. Seed US-60 Permissions
INSERT INTO app_permission (code, description, active) VALUES
    ('DELIVERY_REDELIVERY_SCHEDULE', 'Schedule and reschedule redeliveries for failed delivery orders', TRUE),
    ('DELIVERY_REDELIVERY_VIEW', 'View redelivery history and slot suggestions', TRUE);

