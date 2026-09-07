-- US-46 authorized forward remediation: durable worker-mapping command replay.
CREATE TABLE driver_payroll_worker_mapping_command (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    result_mapping_id UUID NOT NULL,
    result_version BIGINT NOT NULL,
    result_external_system_alias VARCHAR(80) NOT NULL,
    result_external_worker_reference VARCHAR(160) NOT NULL,
    result_active BOOLEAN NOT NULL,
    result_updated_by UUID NOT NULL,
    result_created_at TIMESTAMPTZ NOT NULL,
    result_updated_at TIMESTAMPTZ NOT NULL,
    actor_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_driver_payroll_mapping_command_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_driver_payroll_mapping_command_key UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT ck_driver_payroll_mapping_command_hash
        CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT fk_driver_payroll_mapping_command_result
        FOREIGN KEY (result_mapping_id, tenant_id)
        REFERENCES driver_payroll_worker_mapping (id, tenant_id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_driver_payroll_mapping_command_driver
    ON driver_payroll_worker_mapping_command (tenant_id, driver_id, created_at DESC, id);

CREATE OR REPLACE FUNCTION prevent_driver_payroll_append_only_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END $$;

CREATE TRIGGER trg_driver_payroll_mapping_command_immutable
    BEFORE UPDATE OR DELETE ON driver_payroll_worker_mapping_command
    FOR EACH ROW EXECUTE FUNCTION prevent_driver_payroll_append_only_mutation();

CREATE TRIGGER trg_driver_payroll_history_immutable
    BEFORE UPDATE OR DELETE ON driver_payroll_input_history
    FOR EACH ROW EXECUTE FUNCTION prevent_driver_payroll_append_only_mutation();
