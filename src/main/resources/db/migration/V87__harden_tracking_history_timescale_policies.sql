ALTER TABLE tracking_position_history
 ADD COLUMN event_version INTEGER NOT NULL DEFAULT 1,
 ADD CONSTRAINT ck_tracking_history_event_version CHECK (event_version = 1);

CREATE UNIQUE INDEX uq_tracking_history_dedupe
 ON tracking_position_history(tenant_id, source_timestamp, dedupe_identity);

${trackingTimescalePolicyHardening}
