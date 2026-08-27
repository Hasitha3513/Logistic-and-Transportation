-- V38__load_plan_readiness.sql
-- Add readiness status and audit columns to load_plan table (US-26)

ALTER TABLE load_plan ADD COLUMN readiness_status VARCHAR(40) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE load_plan ADD COLUMN ready_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE load_plan ADD COLUMN ready_by VARCHAR(128);

ALTER TABLE load_plan ADD CONSTRAINT chk_load_plan_readiness_status CHECK (readiness_status IN ('DRAFT', 'STRUCTURALLY_READY'));
ALTER TABLE load_plan ADD CONSTRAINT chk_load_plan_readiness_audit CHECK (
    (readiness_status = 'DRAFT' AND ready_at IS NULL AND ready_by IS NULL) OR
    (readiness_status = 'STRUCTURALLY_READY' AND ready_at IS NOT NULL AND ready_by IS NOT NULL)
);
