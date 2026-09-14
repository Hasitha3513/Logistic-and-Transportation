CREATE TABLE tracking_telemetry_evaluation_dispatch (
 dispatch_id UUID NOT NULL DEFAULT gen_random_uuid(),
 tenant_id UUID NOT NULL,
 source_timestamp TIMESTAMPTZ NOT NULL,
 history_id UUID NOT NULL,
 dedupe_identity CHAR(64) NOT NULL,
 vehicle_id UUID NOT NULL,
 evaluator_type VARCHAR(24) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 attempt_count INTEGER NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMPTZ NOT NULL,
 lease_owner VARCHAR(120),
 lease_until TIMESTAMPTZ,
 completed_at TIMESTAMPTZ,
 last_error_code VARCHAR(120),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 version BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY (dispatch_id),
 CONSTRAINT uq_tracking_telemetry_dispatch_history
  UNIQUE (tenant_id,source_timestamp,history_id,evaluator_type),
 CONSTRAINT uq_tracking_telemetry_dispatch_dedupe
  UNIQUE (tenant_id,source_timestamp,dedupe_identity,evaluator_type),
 CONSTRAINT ck_tracking_telemetry_dispatch_evaluator
  CHECK (evaluator_type IN ('GEOFENCE','SPEED','ROUTE_DEVIATION')),
 CONSTRAINT ck_tracking_telemetry_dispatch_status
  CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED')),
 CONSTRAINT ck_tracking_telemetry_dispatch_attempt
  CHECK (attempt_count BETWEEN 0 AND 1000),
 CONSTRAINT ck_tracking_telemetry_dispatch_lease
  CHECK ((status='PROCESSING' AND lease_owner IS NOT NULL AND lease_until IS NOT NULL)
      OR (status<>'PROCESSING' AND lease_owner IS NULL AND lease_until IS NULL)),
 CONSTRAINT ck_tracking_telemetry_dispatch_completion
  CHECK ((status='COMPLETED')=(completed_at IS NOT NULL)),
 CONSTRAINT ck_tracking_telemetry_dispatch_error
  CHECK (last_error_code IS NULL OR length(btrim(last_error_code)) BETWEEN 1 AND 120)
);

CREATE INDEX idx_tracking_telemetry_dispatch_due
 ON tracking_telemetry_evaluation_dispatch(
  status,next_attempt_at,tenant_id,vehicle_id,source_timestamp,evaluator_type,dispatch_id);
CREATE INDEX idx_tracking_telemetry_dispatch_vehicle_time
 ON tracking_telemetry_evaluation_dispatch(tenant_id,vehicle_id,source_timestamp DESC,dispatch_id);

COMMENT ON TABLE tracking_telemetry_evaluation_dispatch IS
 'Tracking-owned durable evaluation intents keyed to immutable Timescale history identity; the hypertable relationship is transactionally enforced because PostgreSQL/Timescale requires partition columns in referenced uniqueness.';

ALTER TABLE tracking_geofence_transition
 DROP CONSTRAINT fk_tracking_geofence_transition_position;

CREATE FUNCTION validate_tracking_geofence_transition_position() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS (SELECT 1 FROM tracking_position
                 WHERE tenant_id=NEW.tenant_id AND id=NEW.confirming_position_id)
    AND NOT EXISTS (SELECT 1 FROM tracking_position_history
                    WHERE tenant_id=NEW.tenant_id AND id=NEW.confirming_position_id) THEN
  RAISE EXCEPTION 'Geofence transition requires a same-Tenant legacy or immutable history position'
   USING ERRCODE='23503';
 END IF;
 RETURN NEW;
END $$;

CREATE TRIGGER trg_tracking_geofence_transition_position
 BEFORE INSERT ON tracking_geofence_transition
 FOR EACH ROW EXECUTE FUNCTION validate_tracking_geofence_transition_position();
