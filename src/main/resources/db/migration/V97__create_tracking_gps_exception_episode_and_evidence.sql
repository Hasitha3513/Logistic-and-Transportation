-- US-55 CS04: authoritative GPS-exception lifecycle and immutable minimized evidence.
CREATE TABLE tracking_gps_exception_episode (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 tracking_device_id UUID NOT NULL,
 vehicle_id UUID,
 exception_type VARCHAR(32) NOT NULL,
 severity VARCHAR(8) NOT NULL,
 status VARCHAR(16) NOT NULL,
 opened_at TIMESTAMPTZ NOT NULL,
 last_observed_at TIMESTAMPTZ NOT NULL,
 resolved_at TIMESTAMPTZ,
 evidence_count BIGINT NOT NULL,
 consecutive_recovery_points INTEGER NOT NULL DEFAULT 0,
 version BIGINT NOT NULL DEFAULT 0,
 acknowledgement_reason VARCHAR(500),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT uq_tracking_gps_exception_episode_tenant_id UNIQUE (tenant_id,id),
 CONSTRAINT fk_tracking_gps_exception_episode_device
  FOREIGN KEY (tenant_id,tracking_device_id)
  REFERENCES tracking_device(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_tracking_gps_exception_type CHECK (exception_type IN
  ('INVALID_TELEMETRY','CLOCK_ANOMALY','LOW_ACCURACY','IMPOSSIBLE_MOVEMENT',
   'SIGNAL_LOSS','DEVICE_TAMPER','BATTERY_LOW','BATTERY_RAPID_DRAIN',
   'BINDING_VIOLATION','PROCESSING_FAILURE')),
 CONSTRAINT ck_tracking_gps_exception_severity CHECK (severity IN ('WARNING','HIGH')),
 CONSTRAINT ck_tracking_gps_exception_status CHECK
  (status IN ('OPEN','ACKNOWLEDGED','RECOVERING','RESOLVED')),
 CONSTRAINT ck_tracking_gps_exception_time_order CHECK (opened_at<=last_observed_at),
 CONSTRAINT ck_tracking_gps_exception_resolution CHECK
  ((status='RESOLVED' AND resolved_at IS NOT NULL AND resolved_at>=last_observed_at)
   OR (status<>'RESOLVED' AND resolved_at IS NULL)),
 CONSTRAINT ck_tracking_gps_exception_counters CHECK
  (evidence_count>=1 AND consecutive_recovery_points>=0 AND version>=0),
 CONSTRAINT ck_tracking_gps_exception_reason CHECK
  (acknowledgement_reason IS NULL OR
   (length(trim(acknowledgement_reason)) BETWEEN 1 AND 500))
);

CREATE UNIQUE INDEX uq_tracking_gps_exception_active
 ON tracking_gps_exception_episode(tenant_id,tracking_device_id,exception_type)
 WHERE status IN ('OPEN','ACKNOWLEDGED','RECOVERING');
CREATE INDEX idx_tracking_gps_exception_tenant_list
 ON tracking_gps_exception_episode(tenant_id,last_observed_at DESC,id DESC);

CREATE TABLE tracking_gps_exception_evidence (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 episode_id UUID NOT NULL,
 evidence_identity CHAR(64) NOT NULL,
 telemetry_history_id UUID,
 telemetry_source_timestamp TIMESTAMPTZ,
 assessed_at TIMESTAMPTZ NOT NULL,
 trust VARCHAR(12) NOT NULL,
 ordering_classification VARCHAR(20) NOT NULL,
 reliability_state VARCHAR(16) NOT NULL,
 quality_codes VARCHAR(400) NOT NULL,
 transition VARCHAR(20) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT fk_tracking_gps_exception_evidence_episode
  FOREIGN KEY (tenant_id,episode_id)
  REFERENCES tracking_gps_exception_episode(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT uq_tracking_gps_exception_evidence_identity
  UNIQUE (tenant_id,episode_id,evidence_identity),
 CONSTRAINT ck_tracking_gps_exception_evidence_history CHECK
  ((telemetry_history_id IS NULL AND telemetry_source_timestamp IS NULL)
   OR (telemetry_history_id IS NOT NULL AND telemetry_source_timestamp IS NOT NULL)),
 CONSTRAINT ck_tracking_gps_exception_evidence_trust CHECK
  (trust IN ('TRUSTED','UNTRUSTED','UNKNOWN')),
 CONSTRAINT ck_tracking_gps_exception_evidence_ordering CHECK
  (ordering_classification IN ('IN_ORDER','OUT_OF_ORDER','EQUAL_SOURCE_TIME')),
 CONSTRAINT ck_tracking_gps_exception_evidence_state CHECK
  (reliability_state IN ('NORMAL','DEGRADED','SUSPECT','OFFLINE','RECOVERING','UNKNOWN')),
 CONSTRAINT ck_tracking_gps_exception_evidence_transition CHECK
  (transition IN ('OPENED','OBSERVED','RECOVERING','RESOLVED')),
 CONSTRAINT ck_tracking_gps_exception_evidence_quality CHECK
  (length(quality_codes) BETWEEN 1 AND 400)
);

CREATE INDEX idx_tracking_gps_exception_evidence_tenant_episode
 ON tracking_gps_exception_evidence(tenant_id,episode_id,assessed_at DESC,id DESC);

CREATE OR REPLACE FUNCTION prevent_tracking_gps_exception_evidence_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'tracking GPS exception evidence is append-only';
END $$;

CREATE TRIGGER trg_tracking_gps_exception_evidence_immutable
 BEFORE UPDATE OR DELETE ON tracking_gps_exception_evidence
 FOR EACH ROW EXECUTE FUNCTION prevent_tracking_gps_exception_evidence_mutation();
