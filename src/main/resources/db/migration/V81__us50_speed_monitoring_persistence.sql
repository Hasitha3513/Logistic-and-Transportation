CREATE TABLE tracking_speed_rule (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 name VARCHAR(120) NOT NULL,
 scope VARCHAR(16) NOT NULL,
 route_id UUID,
 route_version VARCHAR(120),
 threshold_kph NUMERIC(7,3) NOT NULL,
 lifecycle VARCHAR(16) NOT NULL,
 version BIGINT NOT NULL,
 effective_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT uq_tracking_speed_rule_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT ck_tracking_speed_rule_name CHECK(name=btrim(name) AND length(name)>0),
 CONSTRAINT ck_tracking_speed_rule_scope CHECK(scope IN('TENANT','ROUTE_VERSION')),
 CONSTRAINT ck_tracking_speed_rule_scope_reference CHECK(
  (scope='TENANT' AND route_id IS NULL AND route_version IS NULL)
  OR (scope='ROUTE_VERSION' AND route_id IS NOT NULL AND route_version IS NOT NULL
      AND route_version=btrim(route_version) AND length(route_version)>0)),
 CONSTRAINT ck_tracking_speed_rule_threshold CHECK(threshold_kph>0 AND threshold_kph<=400),
 CONSTRAINT ck_tracking_speed_rule_lifecycle CHECK(lifecycle IN('DRAFT','ACTIVE','DISABLED','RETIRED')),
 CONSTRAINT ck_tracking_speed_rule_version CHECK(version>0),
 CONSTRAINT ck_tracking_speed_rule_effective CHECK(lifecycle<>'ACTIVE' OR effective_at IS NOT NULL)
);
CREATE UNIQUE INDEX uq_tracking_speed_rule_active_tenant
 ON tracking_speed_rule(tenant_id) WHERE scope='TENANT' AND lifecycle='ACTIVE';
CREATE UNIQUE INDEX uq_tracking_speed_rule_active_route
 ON tracking_speed_rule(tenant_id,route_id,route_version)
 WHERE scope='ROUTE_VERSION' AND lifecycle='ACTIVE';
CREATE INDEX idx_tracking_speed_rule_route_lookup
 ON tracking_speed_rule(tenant_id,route_id,route_version)
 INCLUDE(id,threshold_kph,version,effective_at) WHERE lifecycle='ACTIVE';
CREATE INDEX idx_tracking_speed_rule_tenant_lookup
 ON tracking_speed_rule(tenant_id)
 INCLUDE(id,threshold_kph,version,effective_at) WHERE lifecycle='ACTIVE' AND scope='TENANT';

CREATE TABLE tracking_speed_state (
 tenant_id UUID NOT NULL,
 vehicle_id UUID NOT NULL,
 state VARCHAR(16) NOT NULL,
 availability VARCHAR(32) NOT NULL,
 effective_rule_id UUID,
 effective_rule_version BIGINT,
 candidate_position_id UUID,
 candidate_source_timestamp TIMESTAMPTZ,
 candidate_observed_speed_kph NUMERIC(7,3),
 candidate_sample_count SMALLINT NOT NULL DEFAULT 0,
 active_episode_id UUID,
 last_evaluated_source_timestamp TIMESTAMPTZ,
 last_evaluated_position_id UUID,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant_id,vehicle_id),
 CONSTRAINT ck_tracking_speed_state_value CHECK(state IN('UNKNOWN','NORMAL','SPEEDING')),
 CONSTRAINT ck_tracking_speed_state_availability CHECK(
  availability IN('AVAILABLE','NOT_EVALUATED','CONFIGURATION_UNAVAILABLE')),
 CONSTRAINT ck_tracking_speed_state_rule CHECK(
  (effective_rule_id IS NULL AND effective_rule_version IS NULL)
  OR (effective_rule_id IS NOT NULL AND effective_rule_version>0)),
 CONSTRAINT ck_tracking_speed_state_candidate CHECK(
  (candidate_position_id IS NULL AND candidate_source_timestamp IS NULL
   AND candidate_observed_speed_kph IS NULL AND candidate_sample_count=0)
  OR (candidate_position_id IS NOT NULL AND candidate_source_timestamp IS NOT NULL
   AND candidate_observed_speed_kph BETWEEN 0 AND 400 AND candidate_sample_count=1)),
 CONSTRAINT ck_tracking_speed_state_episode CHECK(
  (state='SPEEDING' AND active_episode_id IS NOT NULL)
  OR (state<>'SPEEDING' AND active_episode_id IS NULL)),
 CONSTRAINT ck_tracking_speed_state_last_evaluated CHECK(
  (last_evaluated_source_timestamp IS NULL)=(last_evaluated_position_id IS NULL)),
 CONSTRAINT ck_tracking_speed_state_version CHECK(version>=0)
);

CREATE TABLE tracking_speed_episode (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 vehicle_id UUID NOT NULL,
 trip_id UUID,
 driver_id UUID,
 route_id UUID,
 route_version VARCHAR(120),
 rule_id UUID NOT NULL,
 rule_version BIGINT NOT NULL,
 threshold_source VARCHAR(16) NOT NULL,
 effective_threshold_kph NUMERIC(7,3) NOT NULL,
 start_source_timestamp TIMESTAMPTZ NOT NULL,
 confirmation_source_timestamp TIMESTAMPTZ NOT NULL,
 end_source_timestamp TIMESTAMPTZ,
 max_observed_speed_kph NUMERIC(7,3) NOT NULL,
 eligible_above_threshold_sample_count INTEGER NOT NULL,
 severity VARCHAR(8) NOT NULL,
 repeat_count INTEGER NOT NULL,
 first_candidate_position_id UUID NOT NULL,
 confirming_position_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT uq_tracking_speed_episode_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT ck_tracking_speed_episode_rule_version CHECK(rule_version>0),
 CONSTRAINT ck_tracking_speed_episode_threshold_source CHECK(
  threshold_source IN('ROUTE_CONFIG','TENANT_CONFIG')),
 CONSTRAINT ck_tracking_speed_episode_threshold CHECK(
  effective_threshold_kph>0 AND effective_threshold_kph<=400),
 CONSTRAINT ck_tracking_speed_episode_speed CHECK(
  max_observed_speed_kph>=0 AND max_observed_speed_kph<=400),
 CONSTRAINT ck_tracking_speed_episode_chronology CHECK(
  confirmation_source_timestamp>=start_source_timestamp
  AND (end_source_timestamp IS NULL OR end_source_timestamp>=confirmation_source_timestamp)),
 CONSTRAINT ck_tracking_speed_episode_count CHECK(
  eligible_above_threshold_sample_count>=2 AND repeat_count>=0),
 CONSTRAINT ck_tracking_speed_episode_severity CHECK(severity IN('WARNING','HIGH'))
);
CREATE UNIQUE INDEX uq_tracking_speed_episode_active_vehicle
 ON tracking_speed_episode(tenant_id,vehicle_id) WHERE end_source_timestamp IS NULL;
CREATE INDEX idx_tracking_speed_episode_vehicle_history
 ON tracking_speed_episode(tenant_id,vehicle_id,start_source_timestamp DESC,id DESC);
CREATE INDEX idx_tracking_speed_episode_severity_history
 ON tracking_speed_episode(tenant_id,severity,start_source_timestamp DESC,id DESC);
CREATE INDEX idx_tracking_speed_episode_repeat
 ON tracking_speed_episode(tenant_id,vehicle_id,rule_id,rule_version,end_source_timestamp DESC,id DESC)
 WHERE end_source_timestamp IS NOT NULL;

CREATE OR REPLACE FUNCTION protect_tracking_speed_episode() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN
  RAISE EXCEPTION 'tracking speed episode evidence cannot be deleted';
 END IF;
 IF OLD.end_source_timestamp IS NOT NULL THEN
  RAISE EXCEPTION 'closed tracking speed episode evidence is immutable';
 END IF;
 IF ROW(NEW.id,NEW.tenant_id,NEW.vehicle_id,NEW.trip_id,NEW.driver_id,NEW.route_id,
        NEW.route_version,NEW.rule_id,NEW.rule_version,NEW.threshold_source,
        NEW.effective_threshold_kph,NEW.start_source_timestamp,NEW.confirmation_source_timestamp,
        NEW.severity,NEW.repeat_count,NEW.first_candidate_position_id,NEW.confirming_position_id,
        NEW.created_at)
    IS DISTINCT FROM
    ROW(OLD.id,OLD.tenant_id,OLD.vehicle_id,OLD.trip_id,OLD.driver_id,OLD.route_id,
        OLD.route_version,OLD.rule_id,OLD.rule_version,OLD.threshold_source,
        OLD.effective_threshold_kph,OLD.start_source_timestamp,OLD.confirmation_source_timestamp,
        OLD.severity,OLD.repeat_count,OLD.first_candidate_position_id,OLD.confirming_position_id,
        OLD.created_at) THEN
  RAISE EXCEPTION 'tracking speed episode identity evidence is immutable';
 END IF;
 IF NEW.max_observed_speed_kph<OLD.max_observed_speed_kph
    OR NEW.eligible_above_threshold_sample_count<OLD.eligible_above_threshold_sample_count
    OR (OLD.end_source_timestamp IS NULL AND NEW.end_source_timestamp IS NULL
        AND NEW.updated_at<OLD.updated_at) THEN
  RAISE EXCEPTION 'tracking speed episode progress cannot move backward';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_tracking_speed_episode_protection
BEFORE UPDATE OR DELETE ON tracking_speed_episode
FOR EACH ROW EXECUTE FUNCTION protect_tracking_speed_episode();

CREATE TABLE tracking_speed_evaluation_job (
 tenant_id UUID NOT NULL,
 position_id UUID NOT NULL,
 vehicle_id UUID NOT NULL,
 source_timestamp TIMESTAMPTZ NOT NULL,
 status VARCHAR(16) NOT NULL,
 attempt_count INTEGER NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMPTZ NOT NULL,
 lease_owner VARCHAR(120),
 lease_until TIMESTAMPTZ,
 last_error_code VARCHAR(120),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant_id,position_id),
 CONSTRAINT fk_tracking_speed_job_position FOREIGN KEY(tenant_id,position_id)
  REFERENCES tracking_position(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_tracking_speed_job_status CHECK(status IN('PENDING','PROCESSING','COMPLETED','FAILED')),
 CONSTRAINT ck_tracking_speed_job_attempt CHECK(attempt_count>=0),
 CONSTRAINT ck_tracking_speed_job_lease CHECK(
  (status='PROCESSING' AND lease_owner IS NOT NULL AND lease_until IS NOT NULL)
  OR (status<>'PROCESSING' AND lease_owner IS NULL AND lease_until IS NULL)),
 CONSTRAINT ck_tracking_speed_job_error CHECK(last_error_code IS NULL OR length(btrim(last_error_code))>0)
);
CREATE INDEX idx_tracking_speed_job_global_due
 ON tracking_speed_evaluation_job(next_attempt_at,tenant_id,position_id)
 INCLUDE(status,lease_until);
