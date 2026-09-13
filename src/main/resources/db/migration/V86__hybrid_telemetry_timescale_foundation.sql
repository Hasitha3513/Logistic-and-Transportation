${trackingTimescaleSetup}

CREATE TABLE tracking_position_history (
 tenant_id UUID NOT NULL,
 source_timestamp TIMESTAMPTZ NOT NULL,
 id UUID NOT NULL,
 device_id UUID NOT NULL,
 vehicle_id UUID NOT NULL,
 provider_alias VARCHAR(80) NOT NULL,
 provider_message_id VARCHAR(160),
 provider_sequence BIGINT,
 dedupe_identity CHAR(64) NOT NULL,
 received_at TIMESTAMPTZ NOT NULL,
 latitude NUMERIC(10,7) NOT NULL,
 longitude NUMERIC(10,7) NOT NULL,
 horizontal_accuracy_meters NUMERIC(10,3),
 speed_kph NUMERIC(8,3),
 heading_degrees NUMERIC(7,3),
 altitude_meters NUMERIC(12,3),
 engine_state VARCHAR(10) NOT NULL,
 odometer_km NUMERIC(14,3),
 engine_hours NUMERIC(14,3),
 trust VARCHAR(12) NOT NULL,
 quality VARCHAR(32) NOT NULL,
 ordering_classification VARCHAR(20) NOT NULL,
 retention_policy VARCHAR(80) NOT NULL,
 retention_policy_version VARCHAR(40) NOT NULL,
 retain_until TIMESTAMPTZ,
 safe_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
 PRIMARY KEY (tenant_id, source_timestamp, id),
 CONSTRAINT ck_tracking_history_lat CHECK(latitude BETWEEN -90 AND 90),
 CONSTRAINT ck_tracking_history_lon CHECK(longitude BETWEEN -180 AND 180),
 CONSTRAINT ck_tracking_history_accuracy CHECK(horizontal_accuracy_meters IS NULL OR horizontal_accuracy_meters>=0),
 CONSTRAINT ck_tracking_history_speed CHECK(speed_kph IS NULL OR speed_kph BETWEEN 0 AND 400),
 CONSTRAINT ck_tracking_history_heading CHECK(heading_degrees IS NULL OR (heading_degrees>=0 AND heading_degrees<360)),
 CONSTRAINT ck_tracking_history_engine CHECK(engine_state IN('ON','OFF','UNKNOWN')),
 CONSTRAINT ck_tracking_history_trust CHECK(trust IN('TRUSTED','UNTRUSTED','UNKNOWN')),
 CONSTRAINT ck_tracking_history_order CHECK(ordering_classification IN('IN_ORDER','CLOCK_SKEW','OUT_OF_ORDER','LATE','FUTURE'))
);

${trackingTimescaleHypertable}

CREATE INDEX idx_tracking_history_vehicle_time
 ON tracking_position_history(tenant_id, vehicle_id, source_timestamp DESC, id DESC);
CREATE INDEX idx_tracking_history_device_time
 ON tracking_position_history(tenant_id, device_id, source_timestamp DESC, id DESC);
CREATE INDEX idx_tracking_history_retain_until
 ON tracking_position_history(tenant_id, retain_until)
 WHERE retain_until IS NOT NULL;
