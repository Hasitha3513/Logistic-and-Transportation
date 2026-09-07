-- US-47 dedicated Billing context and Freight-owner terminal billing projection.
ALTER TABLE freight_order ADD CONSTRAINT uq_freight_order_id_tenant UNIQUE(id,tenant_id);

CREATE TABLE freight_billing_fact (
 freight_order_id UUID NOT NULL, tenant_id UUID NOT NULL, lifecycle VARCHAR(16) NOT NULL,
 completed_at TIMESTAMPTZ NOT NULL, source_version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 PRIMARY KEY(freight_order_id), CONSTRAINT uq_freight_billing_fact_tenant_id UNIQUE(tenant_id,freight_order_id),
 CONSTRAINT fk_freight_billing_fact_order_tenant FOREIGN KEY(freight_order_id,tenant_id)
   REFERENCES freight_order(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT ck_freight_billing_fact_lifecycle CHECK(lifecycle IN('COMPLETED','CLOSED'))
);
CREATE INDEX idx_freight_billing_fact_terminal ON freight_billing_fact(tenant_id,lifecycle,completed_at,freight_order_id);

CREATE TABLE transport_billing_record (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, billing_number VARCHAR(16) NOT NULL,
 record_type VARCHAR(12) NOT NULL, original_billing_record_id UUID, replacement_of_record_id UUID,
 source_type VARCHAR(16) NOT NULL, source_id UUID NOT NULL, source_business_number VARCHAR(80) NOT NULL,
 source_terminal_lifecycle VARCHAR(16) NOT NULL, source_completion_time TIMESTAMPTZ NOT NULL,
 source_version BIGINT NOT NULL, source_snapshot_hash CHAR(64) NOT NULL, customer_id UUID NOT NULL,
 currency CHAR(3) NOT NULL, lifecycle VARCHAR(24) NOT NULL,
 base_charge NUMERIC(19,2) NOT NULL DEFAULT 0, surcharges NUMERIC(19,2) NOT NULL DEFAULT 0,
 penalties NUMERIC(19,2) NOT NULL DEFAULT 0, credit_adjustments NUMERIC(19,2) NOT NULL DEFAULT 0,
 subtotal NUMERIC(19,2) NOT NULL DEFAULT 0, tax_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
 total_amount NUMERIC(19,2) NOT NULL DEFAULT 0, prepared_by UUID NOT NULL, approved_by UUID,
 approved_at TIMESTAMPTZ, finalized_at TIMESTAMPTZ, export_configuration_id UUID, export_event_id UUID,
 validation_hash CHAR(64), compliance VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 create_idempotency_key VARCHAR(160) NOT NULL, create_request_hash CHAR(64) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_transport_billing_record_id_tenant UNIQUE(id,tenant_id),
 CONSTRAINT uq_transport_billing_number UNIQUE(tenant_id,billing_number),
 CONSTRAINT uq_transport_billing_create_key UNIQUE(tenant_id,create_idempotency_key),
 CONSTRAINT uq_transport_billing_export_event UNIQUE(tenant_id,export_event_id),
 CONSTRAINT ck_transport_billing_record_type CHECK(record_type IN('REGULAR','REVERSAL')),
 CONSTRAINT ck_transport_billing_original CHECK((record_type='REGULAR' AND original_billing_record_id IS NULL) OR (record_type='REVERSAL' AND original_billing_record_id IS NOT NULL)),
 CONSTRAINT ck_transport_billing_source CHECK(source_type IN('TRIP','FREIGHT_ORDER')),
 CONSTRAINT ck_transport_billing_source_lifecycle CHECK((source_type='TRIP' AND source_terminal_lifecycle='CLOSED') OR (source_type='FREIGHT_ORDER' AND source_terminal_lifecycle IN('COMPLETED','CLOSED'))),
 CONSTRAINT ck_transport_billing_currency CHECK(currency ~ '^[A-Z]{3}$'),
 CONSTRAINT ck_transport_billing_lifecycle CHECK(lifecycle IN('DRAFT','VALIDATED','APPROVED','FINALIZED','EXPORT_REQUESTED','EXPORTED','CANCELLED','REVERSED')),
 CONSTRAINT ck_transport_billing_compliance CHECK(compliance IN('ACCEPTED','NOT_REQUIRED','PENDING')),
 CONSTRAINT ck_transport_billing_regular_total CHECK(record_type='REVERSAL' OR total_amount>=0),
 CONSTRAINT fk_transport_billing_original_tenant FOREIGN KEY(original_billing_record_id,tenant_id) REFERENCES transport_billing_record(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT fk_transport_billing_replacement_tenant FOREIGN KEY(replacement_of_record_id,tenant_id) REFERENCES transport_billing_record(id,tenant_id) ON DELETE RESTRICT
);
CREATE INDEX idx_transport_billing_queue ON transport_billing_record(tenant_id,lifecycle,created_at DESC,id);
CREATE INDEX idx_transport_billing_customer ON transport_billing_record(tenant_id,customer_id,created_at DESC);
CREATE INDEX idx_transport_billing_source ON transport_billing_record(tenant_id,source_type,source_id);

CREATE TABLE transport_billing_line (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, billing_record_id UUID NOT NULL,
 category VARCHAR(24) NOT NULL, reason_code VARCHAR(80) NOT NULL, provenance VARCHAR(160) NOT NULL,
 quantity NUMERIC(19,4) NOT NULL, unit_rate NUMERIC(19,4) NOT NULL, amount NUMERIC(19,2) NOT NULL,
 CONSTRAINT uq_transport_billing_line_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT fk_transport_billing_line_record_tenant FOREIGN KEY(billing_record_id,tenant_id) REFERENCES transport_billing_record(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT ck_transport_billing_line_category CHECK(category IN('BASE_CHARGE','SURCHARGE','PENALTY','CREDIT_ADJUSTMENT')),
 CONSTRAINT ck_transport_billing_line_values CHECK(quantity>=0 AND unit_rate>=0 AND amount>=0)
);
CREATE INDEX idx_transport_billing_line_record ON transport_billing_line(tenant_id,billing_record_id,id);

CREATE TABLE transport_billing_cost_centre (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, billing_record_id UUID NOT NULL, code VARCHAR(80) NOT NULL,
 allocation_percent NUMERIC(7,4) NOT NULL, description VARCHAR(240), source VARCHAR(160) NOT NULL,
 CONSTRAINT uq_transport_billing_cost_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_transport_billing_cost_code UNIQUE(tenant_id,billing_record_id,code),
 CONSTRAINT fk_transport_billing_cost_record_tenant FOREIGN KEY(billing_record_id,tenant_id) REFERENCES transport_billing_record(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT ck_transport_billing_cost_percent CHECK(allocation_percent>0 AND allocation_percent<=100.0000)
);
CREATE INDEX idx_transport_billing_cost_record ON transport_billing_cost_centre(tenant_id,billing_record_id,code);

CREATE TABLE transport_billing_tax_fact (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, billing_record_id UUID NOT NULL, status VARCHAR(16) NOT NULL,
 category VARCHAR(80), jurisdiction_reference VARCHAR(120), taxable_amount NUMERIC(19,2), rate NUMERIC(9,4),
 tax_amount NUMERIC(19,2) NOT NULL DEFAULT 0, exemption_reference VARCHAR(120), provenance VARCHAR(160), snapshot_hash CHAR(64),
 CONSTRAINT uq_transport_billing_tax_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_transport_billing_tax_record UNIQUE(tenant_id,billing_record_id),
 CONSTRAINT fk_transport_billing_tax_record_tenant FOREIGN KEY(billing_record_id,tenant_id) REFERENCES transport_billing_record(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT ck_transport_billing_tax_status CHECK(status IN('SUPPLIED','NOT_SUPPLIED')),
 CONSTRAINT ck_transport_billing_tax_shape CHECK((status='NOT_SUPPLIED' AND category IS NULL AND jurisdiction_reference IS NULL AND taxable_amount IS NULL AND rate IS NULL AND tax_amount=0 AND exemption_reference IS NULL) OR (status='SUPPLIED' AND taxable_amount IS NOT NULL AND rate IS NOT NULL AND provenance IS NOT NULL AND snapshot_hash IS NOT NULL))
);

CREATE TABLE transport_billing_source_claim (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, billing_record_id UUID NOT NULL, source_type VARCHAR(16) NOT NULL,
 source_id UUID NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL,
 released_at TIMESTAMPTZ,
 CONSTRAINT uq_transport_billing_claim_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_transport_billing_claim_record UNIQUE(tenant_id,billing_record_id),
 CONSTRAINT fk_transport_billing_claim_record_tenant FOREIGN KEY(billing_record_id,tenant_id) REFERENCES transport_billing_record(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT ck_transport_billing_claim_release CHECK((active AND released_at IS NULL) OR (NOT active AND released_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_transport_billing_active_source ON transport_billing_source_claim(tenant_id,source_type,source_id) WHERE active;

CREATE TABLE transport_billing_history (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, billing_record_id UUID NOT NULL, action VARCHAR(50) NOT NULL,
 from_state VARCHAR(24), to_state VARCHAR(24), actor_id UUID NOT NULL, detail VARCHAR(500),
 idempotency_scope VARCHAR(30), idempotency_key VARCHAR(160), request_hash CHAR(64), result_version BIGINT,
 created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_transport_billing_history_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT fk_transport_billing_history_record_tenant FOREIGN KEY(billing_record_id,tenant_id) REFERENCES transport_billing_record(id,tenant_id) ON DELETE RESTRICT
);
CREATE UNIQUE INDEX uq_transport_billing_command_key ON transport_billing_history(tenant_id,idempotency_scope,idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_transport_billing_history_record ON transport_billing_history(tenant_id,billing_record_id,created_at,id);

CREATE OR REPLACE FUNCTION prevent_transport_billing_child_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_state VARCHAR(24);
BEGIN
 SELECT lifecycle INTO parent_state FROM transport_billing_record WHERE id=COALESCE(OLD.billing_record_id,NEW.billing_record_id) AND tenant_id=COALESCE(OLD.tenant_id,NEW.tenant_id);
 IF parent_state IN('APPROVED','FINALIZED','EXPORT_REQUESTED','EXPORTED','CANCELLED','REVERSED') THEN RAISE EXCEPTION 'released billing facts are immutable'; END IF;
 RETURN COALESCE(NEW,OLD);
END $$;
CREATE TRIGGER trg_transport_billing_line_immutable BEFORE INSERT OR UPDATE OR DELETE ON transport_billing_line FOR EACH ROW EXECUTE FUNCTION prevent_transport_billing_child_mutation();
CREATE TRIGGER trg_transport_billing_cost_immutable BEFORE INSERT OR UPDATE OR DELETE ON transport_billing_cost_centre FOR EACH ROW EXECUTE FUNCTION prevent_transport_billing_child_mutation();
CREATE TRIGGER trg_transport_billing_tax_immutable BEFORE INSERT OR UPDATE OR DELETE ON transport_billing_tax_fact FOR EACH ROW EXECUTE FUNCTION prevent_transport_billing_child_mutation();

CREATE OR REPLACE FUNCTION prevent_transport_billing_history_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'billing history is append-only'; END $$;
CREATE TRIGGER trg_transport_billing_history_immutable BEFORE UPDATE OR DELETE ON transport_billing_history FOR EACH ROW EXECUTE FUNCTION prevent_transport_billing_history_mutation();

INSERT INTO app_permission(code,description,active) VALUES
 ('BILLING_VIEW','View same-Tenant transport billing records and safe export evidence',TRUE),
 ('BILLING_PREPARE','Prepare, edit, cancel and validate transport billing drafts',TRUE),
 ('BILLING_APPROVE','Independently approve validated transport billing records',TRUE),
 ('BILLING_FINALIZE','Finalize approved regular and reversal billing records',TRUE),
 ('BILLING_EXPORT','Request controlled export of finalized transport billing records',TRUE)
ON CONFLICT(code) DO UPDATE SET description=EXCLUDED.description,active=TRUE;
