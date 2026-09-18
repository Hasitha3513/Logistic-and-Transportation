-- US-51 CS04 prerequisite: durable candidates are not provisional episodes.
ALTER TABLE tracking_idle_state
 ADD COLUMN candidate_id UUID,
 ADD COLUMN reference_history_id UUID,
 ADD COLUMN recovery_started_at TIMESTAMPTZ;

ALTER TABLE tracking_idle_episode ADD COLUMN candidate_id UUID;
CREATE UNIQUE INDEX uq_tracking_idle_episode_candidate
 ON tracking_idle_episode(tenant_id,candidate_id) WHERE candidate_id IS NOT NULL;

-- Preserve any V102 candidate exactly; it remains identifiable as legacy storage.
UPDATE tracking_idle_state
 SET candidate_id=open_episode_id, reference_history_id=NULL
 WHERE state='CANDIDATE' AND open_episode_id IS NOT NULL;
UPDATE tracking_idle_episode SET candidate_id=id
 WHERE lifecycle='CANDIDATE' AND candidate_id IS NULL;

ALTER TABLE tracking_idle_state DROP CONSTRAINT ck_tracking_idle_state_episode;
ALTER TABLE tracking_idle_state ADD CONSTRAINT ck_tracking_idle_state_episode CHECK (
 (state='IDLE' AND open_episode_id IS NOT NULL AND candidate_id IS NULL) OR
 (state='CANDIDATE' AND candidate_id IS NOT NULL) OR
 (state NOT IN ('CANDIDATE','IDLE') AND open_episode_id IS NULL AND candidate_id IS NULL));
CREATE INDEX idx_tracking_idle_state_candidate
 ON tracking_idle_state(tenant_id,candidate_id) WHERE candidate_id IS NOT NULL;

CREATE TABLE tracking_idle_candidate_evidence (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), tenant_id UUID NOT NULL,
 candidate_id UUID NOT NULL, vehicle_id UUID NOT NULL, device_id UUID NOT NULL,
 history_id UUID NOT NULL, source_timestamp TIMESTAMPTZ NOT NULL,
 dedupe_identity CHAR(64) NOT NULL, outcome VARCHAR(24) NOT NULL,
 engine_running_state VARCHAR(16), engine_running_source VARCHAR(40),
 speed_kph NUMERIC(8,3), horizontal_accuracy_meters NUMERIC(10,3),
 adjusted_distance_meters NUMERIC(12,3), credited_delta_seconds INTEGER NOT NULL DEFAULT 0,
 retain_until TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT fk_tracking_idle_candidate_device FOREIGN KEY (tenant_id,device_id)
  REFERENCES tracking_device(tenant_id,id),
 CONSTRAINT uq_tracking_idle_candidate_dedupe UNIQUE
  (tenant_id,candidate_id,source_timestamp,dedupe_identity),
 CONSTRAINT ck_tracking_idle_candidate_outcome CHECK (outcome IN
  ('QUALIFYING','NOT_QUALIFYING','UNKNOWN','CONFLICTING_EVIDENCE')),
 CONSTRAINT ck_tracking_idle_candidate_running_pair CHECK
  ((engine_running_state IS NULL)=(engine_running_source IS NULL)),
 CONSTRAINT ck_tracking_idle_candidate_retention CHECK (retain_until>=source_timestamp),
 CONSTRAINT ck_tracking_idle_candidate_metrics CHECK (
  (speed_kph IS NULL OR speed_kph BETWEEN 0 AND 400) AND
  (horizontal_accuracy_meters IS NULL OR horizontal_accuracy_meters BETWEEN 0 AND 100) AND
  (adjusted_distance_meters IS NULL OR adjusted_distance_meters>=0) AND
  credited_delta_seconds BETWEEN 0 AND 120));
CREATE INDEX idx_tracking_idle_candidate_evidence_lookup
 ON tracking_idle_candidate_evidence(tenant_id,candidate_id,source_timestamp,id);
CREATE INDEX idx_tracking_idle_candidate_evidence_retention
 ON tracking_idle_candidate_evidence(retain_until);

CREATE FUNCTION guard_tracking_idle_candidate_evidence() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' AND OLD.retain_until<=now() THEN RETURN OLD; END IF;
 RAISE EXCEPTION 'tracking_idle_candidate_evidence is append-only within retention' USING ERRCODE='23514';
END $$;
CREATE TRIGGER trg_tracking_idle_candidate_evidence_append_only
 BEFORE UPDATE OR DELETE ON tracking_idle_candidate_evidence
 FOR EACH ROW EXECUTE FUNCTION guard_tracking_idle_candidate_evidence();
