-- US-51 CS07: align bounded idle monitoring and IDLE dispatch queries with Tenant-leading indexes.
CREATE INDEX idx_tracking_idle_state_keyset
    ON tracking_idle_state (
        tenant_id,
        latest_source_timestamp DESC,
        vehicle_id DESC
    );

CREATE INDEX idx_tracking_idle_episode_tenant_keyset
    ON tracking_idle_episode (
        tenant_id,
        start_source_timestamp DESC,
        id DESC
    )
    WHERE lifecycle <> 'CANDIDATE';

CREATE INDEX idx_tracking_telemetry_dispatch_idle_due
    ON tracking_telemetry_evaluation_dispatch (
        next_attempt_at,
        tenant_id,
        vehicle_id,
        source_timestamp,
        dispatch_id
    )
    WHERE evaluator_type = 'IDLE'
      AND status IN ('PENDING', 'FAILED', 'PROCESSING');
