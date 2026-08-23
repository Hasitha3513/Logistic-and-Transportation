-- US-71 durable server inbox for at-most-once offline operation processing.

CREATE TABLE offline_sync_operation (
    operation_id UUID PRIMARY KEY,
    operation_type VARCHAR(64) NOT NULL,
    operation_version INTEGER NOT NULL,
    actor_id UUID NOT NULL,
    client_instance_id UUID NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    result_status VARCHAR(32) NOT NULL,
    result_code VARCHAR(64),
    result_version BIGINT,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_offline_sync_actor FOREIGN KEY (actor_id) REFERENCES app_user (id),
    CONSTRAINT chk_offline_sync_version CHECK (operation_version > 0),
    CONSTRAINT chk_offline_sync_result CHECK (result_status IN ('APPLIED', 'REJECTED', 'CONFLICT'))
);

CREATE INDEX idx_offline_sync_actor_processed
    ON offline_sync_operation (actor_id, processed_at);
CREATE INDEX idx_offline_sync_aggregate_processed
    ON offline_sync_operation (aggregate_type, aggregate_id, processed_at);
