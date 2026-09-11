CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE fuel_exception_correction
    ADD COLUMN owner_module VARCHAR(20),
    ADD COLUMN owner_type VARCHAR(50),
    ADD COLUMN requested_command_type VARCHAR(50),
    ADD COLUMN safe_request_evidence TEXT,
    ADD COLUMN before_snapshot_hash CHAR(64),
    ADD COLUMN request_reason VARCHAR(500),
    ADD COLUMN requested_at TIMESTAMPTZ,
    ADD COLUMN execution_state VARCHAR(20),
    ADD COLUMN idempotency_key UUID;

UPDATE fuel_exception_correction SET
    owner_module = CASE WHEN correction_type = 'VEHICLE_READING_CORRECTION' THEN 'FLEET' ELSE 'FUEL' END,
    owner_type = correction_type,
    requested_command_type = correction_type,
    safe_request_evidence = owner_command,
    before_snapshot_hash = encode(digest(owner_command, 'sha256'), 'hex'),
    request_reason = NULLIF(owner_command::jsonb ->> 'reason', ''),
    requested_at = created_at,
    execution_state = CASE WHEN status IN ('APPLIED','FAILED') THEN status ELSE 'NOT_STARTED' END,
    idempotency_key = id
WHERE owner_module IS NULL;

ALTER TABLE fuel_exception_correction
    ALTER COLUMN owner_module SET NOT NULL,
    ALTER COLUMN owner_type SET NOT NULL,
    ALTER COLUMN requested_command_type SET NOT NULL,
    ALTER COLUMN safe_request_evidence SET NOT NULL,
    ALTER COLUMN before_snapshot_hash SET NOT NULL,
    ALTER COLUMN request_reason SET NOT NULL,
    ALTER COLUMN requested_at SET NOT NULL,
    ALTER COLUMN execution_state SET NOT NULL,
    ALTER COLUMN idempotency_key SET NOT NULL,
    ADD CONSTRAINT ck_fuel_exception_correction_owner CHECK (owner_module IN ('FUEL','FLEET')),
    ADD CONSTRAINT ck_fuel_exception_correction_execution CHECK (execution_state IN ('NOT_STARTED','RUNNING','APPLIED','FAILED')),
    ADD CONSTRAINT uq_fuel_exception_correction_idempotency UNIQUE (tenant_id, idempotency_key);

ALTER TABLE fuel_exception_note ALTER COLUMN note TYPE VARCHAR(2000);

CREATE TABLE fuel_exception_correction_attempt (
    attempt_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    correction_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
    idempotency_key UUID NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL,
    attempted_by UUID NOT NULL,
    result VARCHAR(16) NOT NULL CHECK (result IN ('SUCCESS','FAILED','NOOP_REPLAY')),
    safe_result_reference VARCHAR(255),
    safe_error_code VARCHAR(100),
    result_hash CHAR(64),
    UNIQUE (tenant_id, attempt_id),
    UNIQUE (tenant_id, correction_id, attempt_number),
    FOREIGN KEY (tenant_id, correction_id) REFERENCES fuel_exception_correction (tenant_id, id)
);
CREATE INDEX idx_fuel_exception_attempt_correction
    ON fuel_exception_correction_attempt (tenant_id, correction_id, attempt_number DESC);

CREATE FUNCTION reject_fuel_exception_audit_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Fuel exception audit rows are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_fuel_exception_attempt_append_only
    BEFORE UPDATE OR DELETE ON fuel_exception_correction_attempt
    FOR EACH ROW EXECUTE FUNCTION reject_fuel_exception_audit_mutation();

CREATE TRIGGER trg_fuel_exception_history_append_only
    BEFORE UPDATE OR DELETE ON fuel_exception_history
    FOR EACH ROW EXECUTE FUNCTION reject_fuel_exception_audit_mutation();

UPDATE fuel_exception_case SET source_event_id = id
WHERE category = 'NEGATIVE_BUNKER_BALANCE' AND source_type = 'REJECTED_BUNKER_COMMAND' AND source_event_id IS NULL;

ALTER TABLE fuel_exception_case ADD CONSTRAINT ck_fuel_exception_negative_source_event
    CHECK (category <> 'NEGATIVE_BUNKER_BALANCE' OR source_event_id IS NOT NULL);

ALTER TABLE fuel_exception_operations_handoff
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    ADD COLUMN last_attempt_at TIMESTAMPTZ,
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD COLUMN accepted_at TIMESTAMPTZ;
