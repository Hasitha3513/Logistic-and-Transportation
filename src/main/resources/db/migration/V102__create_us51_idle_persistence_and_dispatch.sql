-- US-51 CS03: durable idle state, episodes, minimized evidence and staged dispatch.
CREATE TABLE tracking_idle_episode (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 tenant_id UUID NOT NULL,
 vehicle_id UUID NOT NULL,
 device_id UUID NOT NULL,
 lifecycle VARCHAR(12) NOT NULL DEFAULT 'CANDIDATE',
 start_source_timestamp TIMESTAMPTZ NOT NULL,
 confirmed_at TIMESTAMPTZ,
 last_source_timestamp TIMESTAMPTZ NOT NULL,
 end_source_timestamp TIMESTAMPTZ,
 end_reason VARCHAR(24),
 credited_seconds BIGINT NOT NULL DEFAULT 0,
 evidence_count INTEGER NOT NULL DEFAULT 0,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT uq_tracking_idle_episode_tenant_id UNIQUE (tenant_id,id),
 CONSTRAINT fk_tracking_idle_episode_device FOREIGN KEY (tenant_id,device_id)
  REFERENCES tracking_device(tenant_id,id),
 CONSTRAINT ck_tracking_idle_episode_lifecycle CHECK (lifecycle IN ('CANDIDATE','CONFIRMED','CLOSED')),
 CONSTRAINT ck_tracking_idle_episode_time CHECK (
  last_source_timestamp>=start_source_timestamp AND
 (confirmed_at IS NULL OR confirmed_at>=start_source_timestamp) AND
  (end_source_timestamp IS NULL OR end_source_timestamp>=start_source_timestamp)),
 CONSTRAINT ck_tracking_idle_episode_confirmation CHECK (
  (lifecycle='CANDIDATE' AND confirmed_at IS NULL) OR
  (lifecycle='CONFIRMED' AND confirmed_at IS NOT NULL) OR lifecycle='CLOSED'),
 CONSTRAINT ck_tracking_idle_episode_closed CHECK (
  (lifecycle<>'CLOSED' AND end_source_timestamp IS NULL AND end_reason IS NULL) OR
  (lifecycle='CLOSED' AND end_source_timestamp IS NOT NULL AND end_reason IS NOT NULL)),
 CONSTRAINT ck_tracking_idle_episode_end_reason CHECK (end_reason IS NULL OR end_reason IN
  ('ENGINE_STOPPED','MOVEMENT','EVIDENCE_GAP','DEVICE_REASSIGNED','CAPABILITY_CHANGED')),
 CONSTRAINT ck_tracking_idle_episode_counters CHECK (credited_seconds>=0 AND evidence_count>=0),
 CONSTRAINT ck_tracking_idle_episode_version CHECK (version>=0)
);

CREATE UNIQUE INDEX uq_tracking_idle_episode_open
 ON tracking_idle_episode(tenant_id,vehicle_id) WHERE lifecycle<>'CLOSED';
CREATE INDEX idx_tracking_idle_episode_vehicle_keyset
 ON tracking_idle_episode(tenant_id,vehicle_id,start_source_timestamp DESC,id DESC);

CREATE TABLE tracking_idle_state (
 tenant_id UUID NOT NULL,
 vehicle_id UUID NOT NULL,
 device_id UUID NOT NULL,
 state VARCHAR(28) NOT NULL,
 capability_state VARCHAR(16) NOT NULL,
 latest_source_timestamp TIMESTAMPTZ NOT NULL,
 candidate_started_at TIMESTAMPTZ,
 last_qualifying_at TIMESTAMPTZ,
 credited_seconds BIGINT NOT NULL DEFAULT 0,
 evidence_count INTEGER NOT NULL DEFAULT 0,
 open_episode_id UUID,
 last_dedupe_identity CHAR(64) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY (tenant_id,vehicle_id),
 CONSTRAINT fk_tracking_idle_state_device FOREIGN KEY (tenant_id,device_id)
  REFERENCES tracking_device(tenant_id,id),
 CONSTRAINT fk_tracking_idle_state_episode FOREIGN KEY (tenant_id,open_episode_id)
  REFERENCES tracking_idle_episode(tenant_id,id),
 CONSTRAINT ck_tracking_idle_state_value CHECK (state IN
  ('UNKNOWN','UNSUPPORTED','NOT_REPORTED','STALE','CONFLICTING_EVIDENCE',
   'CANDIDATE','IDLE','NORMAL')),
 CONSTRAINT ck_tracking_idle_state_capability CHECK (capability_state IN
  ('SUPPORTED','UNSUPPORTED','UNKNOWN')),
 CONSTRAINT ck_tracking_idle_state_candidate CHECK (
  (state IN ('CANDIDATE','IDLE') AND candidate_started_at IS NOT NULL) OR
  (state NOT IN ('CANDIDATE','IDLE') AND candidate_started_at IS NULL)),
 CONSTRAINT ck_tracking_idle_state_episode CHECK (
  (state IN ('CANDIDATE','IDLE') AND open_episode_id IS NOT NULL) OR
  (state NOT IN ('CANDIDATE','IDLE') AND open_episode_id IS NULL)),
 CONSTRAINT ck_tracking_idle_state_counters CHECK (credited_seconds>=0 AND evidence_count>=0),
 CONSTRAINT ck_tracking_idle_state_version CHECK (version>=0)
);

CREATE INDEX idx_tracking_idle_state_tenant_state
 ON tracking_idle_state(tenant_id,state,latest_source_timestamp DESC,vehicle_id);

CREATE TABLE tracking_idle_episode_evidence (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 tenant_id UUID NOT NULL,
 episode_id UUID NOT NULL,
 vehicle_id UUID NOT NULL,
 device_id UUID NOT NULL,
 history_id UUID NOT NULL,
 source_timestamp TIMESTAMPTZ NOT NULL,
 dedupe_identity CHAR(64) NOT NULL,
 outcome VARCHAR(24) NOT NULL,
 engine_running_state VARCHAR(16),
 engine_running_source VARCHAR(40),
 speed_kph NUMERIC(8,3),
 horizontal_accuracy_meters NUMERIC(10,3),
 adjusted_distance_meters NUMERIC(12,3),
 credited_delta_seconds INTEGER NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT fk_tracking_idle_evidence_episode FOREIGN KEY (tenant_id,episode_id)
  REFERENCES tracking_idle_episode(tenant_id,id),
 CONSTRAINT fk_tracking_idle_evidence_device FOREIGN KEY (tenant_id,device_id)
  REFERENCES tracking_device(tenant_id,id),
 CONSTRAINT uq_tracking_idle_evidence_dedupe UNIQUE
  (tenant_id,episode_id,source_timestamp,dedupe_identity),
 CONSTRAINT ck_tracking_idle_evidence_outcome CHECK (outcome IN
  ('QUALIFYING','NOT_QUALIFYING','UNKNOWN','CONFLICTING_EVIDENCE')),
 CONSTRAINT ck_tracking_idle_evidence_running_state CHECK (engine_running_state IS NULL OR
  engine_running_state IN ('RUNNING','NOT_RUNNING','UNKNOWN')),
 CONSTRAINT ck_tracking_idle_evidence_running_source CHECK (engine_running_source IS NULL OR
  engine_running_source IN ('DEVICE_NATIVE_CAN','DEVICE_NATIVE_RPM','DEVICE_NATIVE_STATUS',
   'PROVIDER_VERIFIED_DERIVATION')),
 CONSTRAINT ck_tracking_idle_evidence_running_pair CHECK (
  (engine_running_state IS NULL)=(engine_running_source IS NULL)),
 CONSTRAINT ck_tracking_idle_evidence_metrics CHECK (
  (speed_kph IS NULL OR speed_kph BETWEEN 0 AND 400) AND
  (horizontal_accuracy_meters IS NULL OR horizontal_accuracy_meters BETWEEN 0 AND 100) AND
  (adjusted_distance_meters IS NULL OR adjusted_distance_meters>=0) AND
  credited_delta_seconds BETWEEN 0 AND 120)
);

CREATE FUNCTION reject_tracking_idle_evidence_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'tracking_idle_episode_evidence is append-only' USING ERRCODE='23514';
END $$;
CREATE TRIGGER trg_tracking_idle_evidence_append_only
 BEFORE UPDATE OR DELETE ON tracking_idle_episode_evidence
 FOR EACH ROW EXECUTE FUNCTION reject_tracking_idle_evidence_mutation();

ALTER TABLE tracking_telemetry_evaluation_dispatch
 DROP CONSTRAINT ck_tracking_telemetry_dispatch_evaluator,
 ADD CONSTRAINT ck_tracking_telemetry_dispatch_evaluator
  CHECK (evaluator_type IN ('GEOFENCE','SPEED','ROUTE_DEVIATION','IDLE'));
