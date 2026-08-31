-- ============================================================================
-- Migration: V50__delivery_analytics_us61.sql
-- Module: Delivery (MVP 1.3 - US-61 Analyze Delivery Performance)
-- ============================================================================

-- 1. Supporting composite indexes for tenant-scoped analytics queries
CREATE INDEX IF NOT EXISTS idx_delivery_order_tenant_created_status 
    ON delivery_order(tenant_id, created_at, status);

CREATE INDEX IF NOT EXISTS idx_delivery_order_tenant_dest_loc 
    ON delivery_order(tenant_id, destination_location_id);

CREATE INDEX IF NOT EXISTS idx_delivery_attempt_tenant_reason 
    ON delivery_attempt(tenant_id, failure_reason, attempt_timestamp);

-- 2. Seed US-61 Permission
INSERT INTO app_permission (code, description, active) VALUES
    ('DELIVERY_ANALYTICS_VIEW', 'View delivery performance analytics, trends, and KPIs', TRUE)
ON CONFLICT (code) DO UPDATE SET description = EXCLUDED.description, active = TRUE;
