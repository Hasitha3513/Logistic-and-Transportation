-- US-46 Driver-owned operational payroll-input batches and controlled Integration family.
ALTER TABLE integration_configuration DROP CONSTRAINT ck_integration_configuration_classification;
ALTER TABLE integration_configuration ADD CONSTRAINT ck_integration_configuration_classification
    CHECK (data_classification IN ('INTERNAL_OPERATIONAL_NON_SENSITIVE','FINANCIAL_CONFIDENTIAL'));

CREATE TABLE driver_payroll_input_batch (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, batch_type VARCHAR(16) NOT NULL,
 correction_of_batch_id UUID, period_start DATE NOT NULL, period_end_exclusive DATE NOT NULL,
 cutoff_at TIMESTAMPTZ NOT NULL, currency CHAR(3) NOT NULL, lifecycle VARCHAR(24) NOT NULL,
 trip_earnings NUMERIC(19,2) NOT NULL DEFAULT 0, allowances NUMERIC(19,2) NOT NULL DEFAULT 0,
 overtime NUMERIC(19,2) NOT NULL DEFAULT 0, deductions NUMERIC(19,2) NOT NULL DEFAULT 0,
 provisional_net_input NUMERIC(19,2) NOT NULL DEFAULT 0, prepared_by UUID NOT NULL,
 approved_by UUID, approved_at TIMESTAMPTZ, export_configuration_id UUID, export_event_id UUID,
 validation_hash VARCHAR(64), idempotency_key VARCHAR(160) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_driver_payroll_batch_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_driver_payroll_batch_idempotency UNIQUE(tenant_id,idempotency_key),
 CONSTRAINT uq_driver_payroll_export_event UNIQUE(tenant_id,export_event_id),
 CONSTRAINT ck_driver_payroll_batch_type CHECK(batch_type IN('REGULAR','CORRECTION')),
 CONSTRAINT ck_driver_payroll_correction CHECK((batch_type='REGULAR' AND correction_of_batch_id IS NULL) OR (batch_type='CORRECTION' AND correction_of_batch_id IS NOT NULL)),
 CONSTRAINT ck_driver_payroll_period CHECK(period_start < period_end_exclusive),
 CONSTRAINT ck_driver_payroll_currency CHECK(currency ~ '^[A-Z]{3}$'),
 CONSTRAINT ck_driver_payroll_lifecycle CHECK(lifecycle IN('DRAFT','VALIDATED','APPROVED','EXPORT_REQUESTED','EXPORTED','SUPERSEDED')),
 CONSTRAINT ck_driver_payroll_positive_totals CHECK(trip_earnings>=0 AND allowances>=0 AND overtime>=0 AND deductions>=0),
 CONSTRAINT fk_driver_payroll_correction_tenant FOREIGN KEY(correction_of_batch_id,tenant_id) REFERENCES driver_payroll_input_batch(id,tenant_id) ON DELETE RESTRICT
);
CREATE INDEX idx_driver_payroll_period ON driver_payroll_input_batch(tenant_id,period_start,period_end_exclusive);
CREATE INDEX idx_driver_payroll_queue ON driver_payroll_input_batch(tenant_id,lifecycle,batch_type,created_at DESC,id);
CREATE INDEX idx_driver_payroll_export ON driver_payroll_input_batch(tenant_id,lifecycle,export_event_id);

CREATE TABLE driver_payroll_input_line (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, batch_id UUID NOT NULL, driver_id UUID NOT NULL,
 trip_id UUID NOT NULL, trip_number VARCHAR(80) NOT NULL, category VARCHAR(24) NOT NULL,
 reason_code VARCHAR(80) NOT NULL, description VARCHAR(500) NOT NULL, quantity NUMERIC(19,6) NOT NULL,
 unit VARCHAR(12) NOT NULL, rate NUMERIC(19,2) NOT NULL, amount NUMERIC(19,2) NOT NULL,
 original_line_id UUID, source_snapshot_hash VARCHAR(64), external_worker_reference VARCHAR(160),
 CONSTRAINT uq_driver_payroll_line_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT fk_driver_payroll_line_batch_tenant FOREIGN KEY(batch_id,tenant_id) REFERENCES driver_payroll_input_batch(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT fk_driver_payroll_original_line_tenant FOREIGN KEY(original_line_id,tenant_id) REFERENCES driver_payroll_input_line(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT ck_driver_payroll_category CHECK(category IN('TRIP_EARNING','ALLOWANCE','OVERTIME','DEDUCTION')),
 CONSTRAINT ck_driver_payroll_unit CHECK(unit IN('TRIP','HOUR','FIXED')),
 CONSTRAINT ck_driver_payroll_line_values CHECK(quantity>=0 AND rate>=0 AND amount>=0),
 CONSTRAINT ck_driver_payroll_line_hash CHECK(source_snapshot_hash IS NULL OR source_snapshot_hash ~ '^[0-9a-f]{64}$')
);
CREATE UNIQUE INDEX uq_driver_payroll_regular_source ON driver_payroll_input_line(tenant_id,driver_id,trip_id,category) WHERE original_line_id IS NULL;
CREATE UNIQUE INDEX uq_driver_payroll_correction_source ON driver_payroll_input_line(tenant_id,driver_id,trip_id,category,original_line_id) WHERE original_line_id IS NOT NULL;
CREATE INDEX idx_driver_payroll_line_batch ON driver_payroll_input_line(tenant_id,batch_id,id);
CREATE INDEX idx_driver_payroll_line_driver ON driver_payroll_input_line(tenant_id,driver_id,trip_id,category);

CREATE TABLE driver_payroll_worker_mapping (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, driver_id UUID NOT NULL, external_system_alias VARCHAR(80) NOT NULL,
 external_worker_reference VARCHAR(160) NOT NULL, active BOOLEAN NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 updated_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_driver_payroll_mapping_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_driver_payroll_mapping_driver UNIQUE(tenant_id,driver_id),
 CONSTRAINT uq_driver_payroll_mapping_external UNIQUE(tenant_id,external_system_alias,external_worker_reference)
);
CREATE INDEX idx_driver_payroll_mapping_lookup ON driver_payroll_worker_mapping(tenant_id,driver_id,active);

CREATE TABLE driver_payroll_input_history (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, batch_id UUID NOT NULL, action VARCHAR(50) NOT NULL,
 from_state VARCHAR(24), to_state VARCHAR(24), actor_id UUID NOT NULL, detail VARCHAR(500), created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_driver_payroll_history_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT fk_driver_payroll_history_batch_tenant FOREIGN KEY(batch_id,tenant_id) REFERENCES driver_payroll_input_batch(id,tenant_id) ON DELETE RESTRICT
);
CREATE INDEX idx_driver_payroll_history ON driver_payroll_input_history(tenant_id,batch_id,created_at,id);

CREATE OR REPLACE FUNCTION prevent_released_payroll_line_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE batch_state VARCHAR(24);
BEGIN
 SELECT lifecycle INTO batch_state FROM driver_payroll_input_batch WHERE id=COALESCE(OLD.batch_id,NEW.batch_id) AND tenant_id=COALESCE(OLD.tenant_id,NEW.tenant_id);
 IF batch_state IN('APPROVED','EXPORT_REQUESTED','EXPORTED','SUPERSEDED') THEN RAISE EXCEPTION 'released payroll lines are immutable'; END IF;
 RETURN COALESCE(NEW,OLD);
END $$;
CREATE TRIGGER trg_driver_payroll_line_immutable BEFORE UPDATE OR DELETE ON driver_payroll_input_line FOR EACH ROW EXECUTE FUNCTION prevent_released_payroll_line_mutation();

INSERT INTO app_permission(code,description,active) VALUES
 ('DRIVER_PAYROLL_VIEW','View tenant-scoped Driver payroll-input batches and safe delivery state',TRUE),
 ('DRIVER_PAYROLL_PREPARE','Prepare, map, and validate Driver payroll-input batches',TRUE),
 ('DRIVER_PAYROLL_APPROVE','Independently approve Driver payroll-input batches',TRUE),
 ('DRIVER_PAYROLL_EXPORT','Release approved Driver payroll-input batches to Integration',TRUE)
ON CONFLICT(code) DO UPDATE SET description=EXCLUDED.description,active=TRUE;
