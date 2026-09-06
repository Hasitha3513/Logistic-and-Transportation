CREATE TABLE fuel_exception_case (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    category VARCHAR(40) NOT NULL CHECK (category IN ('SUSPECTED_FUEL_LOSS','INCORRECT_READING','SUDDEN_PRICE_CHANGE','EMERGENCY_REFUEL','FUEL_CARD_POLICY_DEVIATION','NEGATIVE_BUNKER_BALANCE')),
    lifecycle VARCHAR(24) NOT NULL CHECK (lifecycle IN ('OPEN','UNDER_REVIEW','CORRECTION_PENDING','AWAITING_APPROVAL','RESOLVED')),
    impact VARCHAR(10) NOT NULL CHECK (impact IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    source_type VARCHAR(40) NOT NULL,
    source_id UUID NOT NULL,
    source_event_id UUID,
    summary VARCHAR(500) NOT NULL,
    safe_metadata TEXT NOT NULL DEFAULT '{}',
    vehicle_id UUID, driver_id UUID, trip_id UUID, card_id UUID, tank_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    review_required BOOLEAN NOT NULL DEFAULT TRUE,
    handoff_status VARCHAR(16) NOT NULL DEFAULT 'NOT_REQUIRED' CHECK (handoff_status IN ('NOT_REQUIRED','PENDING','PUBLISHED','ACCEPTED','FAILED')),
    resolution_outcome VARCHAR(40),
    resolution_reason VARCHAR(500),
    created_by UUID NOT NULL,
    resolved_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, id)
);
ALTER TABLE operational_exception_case DROP CONSTRAINT ck_operational_exception_source_module;
ALTER TABLE operational_exception_case ADD CONSTRAINT ck_operational_exception_source_module
    CHECK (source_module IN ('ROUTING', 'DELIVERY', 'FUEL'));
CREATE UNIQUE INDEX uq_fuel_exception_active_source ON fuel_exception_case (tenant_id, category, source_type, source_id) WHERE lifecycle <> 'RESOLVED';
CREATE UNIQUE INDEX uq_fuel_exception_source_event ON fuel_exception_case (tenant_id, source_event_id) WHERE source_event_id IS NOT NULL;
CREATE INDEX idx_fuel_exception_queue ON fuel_exception_case (tenant_id, category, lifecycle, created_at DESC, id);
CREATE INDEX idx_fuel_exception_source ON fuel_exception_case (tenant_id, source_type, source_id);
CREATE INDEX idx_fuel_exception_vehicle ON fuel_exception_case (tenant_id, vehicle_id) WHERE vehicle_id IS NOT NULL;
CREATE INDEX idx_fuel_exception_driver ON fuel_exception_case (tenant_id, driver_id) WHERE driver_id IS NOT NULL;
CREATE INDEX idx_fuel_exception_card ON fuel_exception_case (tenant_id, card_id) WHERE card_id IS NOT NULL;
CREATE INDEX idx_fuel_exception_tank ON fuel_exception_case (tenant_id, tank_id) WHERE tank_id IS NOT NULL;
CREATE INDEX idx_fuel_exception_review ON fuel_exception_case (tenant_id, review_required, updated_at DESC);

CREATE TABLE fuel_exception_evidence (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL, exception_id UUID NOT NULL,
    evidence_type VARCHAR(40) NOT NULL, source_type VARCHAR(40), source_id UUID,
    summary VARCHAR(500) NOT NULL, safe_snapshot TEXT NOT NULL DEFAULT '{}',
    added_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, exception_id) REFERENCES fuel_exception_case (tenant_id, id)
);
CREATE INDEX idx_fuel_exception_evidence ON fuel_exception_evidence (tenant_id, exception_id, created_at DESC, id);

CREATE TABLE fuel_exception_note (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL, exception_id UUID NOT NULL,
    note VARCHAR(1000) NOT NULL, added_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, exception_id) REFERENCES fuel_exception_case (tenant_id, id)
);
CREATE INDEX idx_fuel_exception_note ON fuel_exception_note (tenant_id, exception_id, created_at DESC, id);

CREATE TABLE fuel_exception_correction (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL, exception_id UUID NOT NULL,
    correction_type VARCHAR(50) NOT NULL, owner_command TEXT NOT NULL,
    changes_financial_fact BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(24) NOT NULL CHECK (status IN ('REQUESTED','AWAITING_APPROVAL','APPROVED','REJECTED','APPLIED','FAILED')),
    requested_by UUID NOT NULL, reviewed_by UUID, review_reason VARCHAR(500),
    owner_result_reference VARCHAR(255), failure_code VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, exception_id) REFERENCES fuel_exception_case (tenant_id, id)
);
CREATE INDEX idx_fuel_exception_correction ON fuel_exception_correction (tenant_id, exception_id, created_at DESC, id);

CREATE TABLE fuel_exception_history (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL, exception_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, from_lifecycle VARCHAR(24), to_lifecycle VARCHAR(24),
    detail VARCHAR(500), actor_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, exception_id) REFERENCES fuel_exception_case (tenant_id, id)
);
CREATE INDEX idx_fuel_exception_history ON fuel_exception_history (tenant_id, exception_id, created_at DESC, id);

CREATE TABLE fuel_exception_operations_handoff (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL, exception_id UUID NOT NULL,
    handoff_event_id UUID NOT NULL, status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','PUBLISHED','ACCEPTED','FAILED')),
    reason VARCHAR(500), failure_code VARCHAR(100), version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, id), UNIQUE (tenant_id, exception_id), UNIQUE (tenant_id, handoff_event_id),
    FOREIGN KEY (tenant_id, exception_id) REFERENCES fuel_exception_case (tenant_id, id)
);
CREATE INDEX idx_fuel_exception_handoff ON fuel_exception_operations_handoff (tenant_id, status, updated_at DESC);

INSERT INTO app_permission (code, description, active)
SELECT code, description, TRUE FROM (VALUES
 ('FUEL_EXCEPTION_VIEW','View Fuel exception cases and safe evidence'),
 ('FUEL_EXCEPTION_MANAGE','Create, review, note, and resolve Fuel exception cases'),
 ('FUEL_EXCEPTION_CORRECT','Request and execute owner-controlled Fuel corrections'),
 ('FUEL_EXCEPTION_APPROVE','Approve or reject independently controlled Fuel corrections'),
 ('FUEL_EXCEPTION_ESCALATE','Escalate qualified Fuel exceptions to Operations')
) AS p(code, description)
ON CONFLICT (code) DO UPDATE SET description=EXCLUDED.description, active=EXCLUDED.active;
