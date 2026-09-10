CREATE TABLE tracking_geofence (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 name VARCHAR(160) NOT NULL,
 type VARCHAR(24) NOT NULL,
 polygon_vertices JSONB NOT NULL,
 min_longitude NUMERIC(10,7) NOT NULL,
 max_longitude NUMERIC(10,7) NOT NULL,
 min_latitude NUMERIC(10,7) NOT NULL,
 max_latitude NUMERIC(10,7) NOT NULL,
 location_id UUID,
 alert_enter_enabled BOOLEAN NOT NULL,
 alert_exit_enabled BOOLEAN NOT NULL,
 lifecycle VARCHAR(16) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL,
 created_by UUID NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 updated_by UUID NOT NULL,
 CONSTRAINT uq_tracking_geofence_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_tracking_geofence_tenant_name UNIQUE(tenant_id,name),
 CONSTRAINT ck_tracking_geofence_name CHECK(name=btrim(name) AND length(name)>0),
 CONSTRAINT ck_tracking_geofence_type CHECK(type IN('DEPOT','CUSTOMER_SITE','UNAUTHORIZED_ZONE')),
 CONSTRAINT ck_tracking_geofence_lifecycle CHECK(lifecycle IN('DRAFT','ACTIVE','DISABLED','RETIRED')),
 CONSTRAINT ck_tracking_geofence_version CHECK(version>=0),
 CONSTRAINT ck_tracking_geofence_bbox CHECK(
  min_longitude BETWEEN -180 AND 180 AND max_longitude BETWEEN -180 AND 180
  AND min_latitude BETWEEN -90 AND 90 AND max_latitude BETWEEN -90 AND 90
  AND min_longitude<=max_longitude AND min_latitude<=max_latitude),
 CONSTRAINT ck_tracking_geofence_location CHECK(
  (type IN('DEPOT','CUSTOMER_SITE') AND location_id IS NOT NULL)
  OR (type='UNAUTHORIZED_ZONE' AND location_id IS NULL)),
 CONSTRAINT ck_tracking_geofence_unauthorized_alert CHECK(
  type<>'UNAUTHORIZED_ZONE' OR alert_enter_enabled),
 CONSTRAINT ck_tracking_geofence_polygon CHECK(
  jsonb_typeof(polygon_vertices)='array'
  AND jsonb_array_length(polygon_vertices) BETWEEN 4 AND 101
  AND octet_length(polygon_vertices::text)<=16384)
);
CREATE INDEX idx_tracking_geofence_active_bbox ON tracking_geofence(
 tenant_id,lifecycle,min_longitude,max_longitude,min_latitude,max_latitude);
CREATE INDEX idx_tracking_geofence_location ON tracking_geofence(tenant_id,location_id);

CREATE TABLE tracking_vehicle_geofence_state (
 tenant_id UUID NOT NULL,
 geofence_id UUID NOT NULL,
 vehicle_id UUID NOT NULL,
 definition_version BIGINT NOT NULL,
 stable_state VARCHAR(8),
 pending_candidate VARCHAR(8),
 pending_count SMALLINT NOT NULL DEFAULT 0,
 pending_position_id UUID,
 last_evaluated_position_id UUID,
 last_evaluated_source_timestamp TIMESTAMPTZ,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(tenant_id,geofence_id,vehicle_id),
 CONSTRAINT fk_tracking_geofence_state_definition FOREIGN KEY(tenant_id,geofence_id)
  REFERENCES tracking_geofence(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_tracking_geofence_state_definition_version CHECK(definition_version>=0),
 CONSTRAINT ck_tracking_geofence_stable CHECK(stable_state IS NULL OR stable_state IN('INSIDE','OUTSIDE')),
 CONSTRAINT ck_tracking_geofence_pending CHECK(pending_candidate IS NULL OR pending_candidate IN('INSIDE','OUTSIDE')),
 CONSTRAINT ck_tracking_geofence_pending_complete CHECK(
  (pending_candidate IS NULL AND pending_count=0 AND pending_position_id IS NULL)
  OR (stable_state IS NOT NULL AND pending_candidate IS NOT NULL AND pending_count=1
      AND pending_position_id IS NOT NULL)),
 CONSTRAINT ck_tracking_geofence_last_evaluated CHECK(
  (last_evaluated_position_id IS NULL)=(last_evaluated_source_timestamp IS NULL)),
 CONSTRAINT ck_tracking_geofence_state_version CHECK(version>=0)
);
CREATE INDEX idx_tracking_geofence_state_vehicle ON tracking_vehicle_geofence_state(
 tenant_id,vehicle_id,geofence_id);

CREATE TABLE tracking_geofence_transition (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 geofence_id UUID NOT NULL,
 vehicle_id UUID NOT NULL,
 location_id UUID,
 geofence_type VARCHAR(24) NOT NULL,
 transition VARCHAR(32) NOT NULL,
 severity VARCHAR(8) NOT NULL,
 source_timestamp TIMESTAMPTZ NOT NULL,
 definition_version BIGINT NOT NULL,
 confirming_position_id UUID NOT NULL,
 from_state VARCHAR(8) NOT NULL,
 to_state VARCHAR(8) NOT NULL,
 transition_identity UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_tracking_geofence_transition_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_tracking_geofence_transition_identity UNIQUE(tenant_id,transition_identity),
 CONSTRAINT fk_tracking_geofence_transition_definition FOREIGN KEY(tenant_id,geofence_id)
  REFERENCES tracking_geofence(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT fk_tracking_geofence_transition_position FOREIGN KEY(tenant_id,confirming_position_id)
  REFERENCES tracking_position(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_tracking_geofence_transition_type CHECK(geofence_type IN('DEPOT','CUSTOMER_SITE','UNAUTHORIZED_ZONE')),
 CONSTRAINT ck_tracking_geofence_transition_value CHECK(transition IN('ENTERED','EXITED','UNAUTHORIZED_ZONE_ENTERED')),
 CONSTRAINT ck_tracking_geofence_transition_severity CHECK(severity IN('NORMAL','HIGH')),
 CONSTRAINT ck_tracking_geofence_transition_states CHECK(from_state IN('INSIDE','OUTSIDE') AND to_state IN('INSIDE','OUTSIDE') AND from_state<>to_state),
 CONSTRAINT ck_tracking_geofence_transition_version CHECK(definition_version>=0)
);
CREATE INDEX idx_tracking_geofence_transition_geofence ON tracking_geofence_transition(
 tenant_id,geofence_id,source_timestamp DESC,id DESC);
CREATE INDEX idx_tracking_geofence_transition_vehicle ON tracking_geofence_transition(
 tenant_id,vehicle_id,source_timestamp DESC,id DESC);
CREATE INDEX idx_tracking_geofence_transition_unauthorized ON tracking_geofence_transition(
 tenant_id,source_timestamp DESC,id DESC) WHERE transition='UNAUTHORIZED_ZONE_ENTERED';

CREATE OR REPLACE FUNCTION prevent_tracking_geofence_transition_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'tracking geofence transition history is append-only'; END $$;
CREATE TRIGGER trg_tracking_geofence_transition_immutable
BEFORE UPDATE OR DELETE ON tracking_geofence_transition
FOR EACH ROW EXECUTE FUNCTION prevent_tracking_geofence_transition_mutation();

CREATE TABLE tracking_geofence_evaluation_job (
 tenant_id UUID NOT NULL,
 position_id UUID NOT NULL,
 status VARCHAR(16) NOT NULL,
 attempt INTEGER NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMPTZ NOT NULL,
 lease_owner VARCHAR(120),
 lease_until TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(tenant_id,position_id),
 CONSTRAINT fk_tracking_geofence_job_position FOREIGN KEY(tenant_id,position_id)
  REFERENCES tracking_position(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_tracking_geofence_job_status CHECK(status IN('PENDING','PROCESSING','COMPLETED','FAILED')),
 CONSTRAINT ck_tracking_geofence_job_attempt CHECK(attempt>=0),
 CONSTRAINT ck_tracking_geofence_job_lease CHECK((lease_owner IS NULL)=(lease_until IS NULL))
);
CREATE INDEX idx_tracking_geofence_job_due ON tracking_geofence_evaluation_job(
 tenant_id,status,next_attempt_at,lease_until,position_id);
