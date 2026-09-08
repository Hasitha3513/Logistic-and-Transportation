CREATE TABLE tracking_device (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, external_device_reference VARCHAR(160) NOT NULL,
 provider_alias VARCHAR(80) NOT NULL, hardware_serial_reference VARCHAR(160), lifecycle VARCHAR(16) NOT NULL,
 registered_at TIMESTAMPTZ NOT NULL, registered_by UUID NOT NULL, last_seen_at TIMESTAMPTZ,
 version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_tracking_device_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_tracking_device_external UNIQUE(tenant_id,provider_alias,external_device_reference),
 CONSTRAINT ck_tracking_device_lifecycle CHECK(lifecycle IN('ACTIVE','DISABLED'))
);
CREATE INDEX idx_tracking_device_list ON tracking_device(tenant_id,created_at DESC,id);

CREATE TABLE tracking_vehicle_device_assignment (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, tracking_device_id UUID NOT NULL, vehicle_id UUID NOT NULL,
 effective_from TIMESTAMPTZ NOT NULL, effective_to TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL, created_by UUID NOT NULL,
 CONSTRAINT uq_tracking_assignment_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT fk_tracking_assignment_device FOREIGN KEY(tracking_device_id,tenant_id) REFERENCES tracking_device(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT ck_tracking_assignment_interval CHECK(effective_to IS NULL OR effective_to>effective_from)
);
CREATE UNIQUE INDEX uq_tracking_active_device ON tracking_vehicle_device_assignment(tenant_id,tracking_device_id) WHERE effective_to IS NULL;
CREATE UNIQUE INDEX uq_tracking_active_vehicle ON tracking_vehicle_device_assignment(tenant_id,vehicle_id) WHERE effective_to IS NULL;
CREATE INDEX idx_tracking_assignment_source_time ON tracking_vehicle_device_assignment(tenant_id,tracking_device_id,effective_from,effective_to);

CREATE OR REPLACE FUNCTION reject_tracking_assignment_overlap() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS (
  SELECT 1 FROM tracking_vehicle_device_assignment existing
  WHERE existing.tenant_id=NEW.tenant_id AND existing.id<>NEW.id
    AND (existing.tracking_device_id=NEW.tracking_device_id OR existing.vehicle_id=NEW.vehicle_id)
    AND tstzrange(existing.effective_from,existing.effective_to,'[)') &&
        tstzrange(NEW.effective_from,NEW.effective_to,'[)')
 ) THEN
  RAISE EXCEPTION 'tracking association interval overlaps existing history'
   USING ERRCODE='23P01';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_tracking_assignment_no_overlap
BEFORE INSERT OR UPDATE OF effective_to ON tracking_vehicle_device_assignment
FOR EACH ROW EXECUTE FUNCTION reject_tracking_assignment_overlap();

CREATE TABLE tracking_position (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, device_id UUID NOT NULL, vehicle_id UUID NOT NULL,
 provider_alias VARCHAR(80) NOT NULL, provider_message_id VARCHAR(160), provider_sequence BIGINT,
 dedupe_identity CHAR(64) NOT NULL, payload_hash CHAR(64) NOT NULL, source_timestamp TIMESTAMPTZ NOT NULL,
 received_at TIMESTAMPTZ NOT NULL, latitude NUMERIC(10,7) NOT NULL, longitude NUMERIC(10,7) NOT NULL,
 horizontal_accuracy_meters NUMERIC(10,3), speed_kph NUMERIC(8,3), heading_degrees NUMERIC(7,3),
 altitude_meters NUMERIC(12,3), engine_state VARCHAR(10) NOT NULL, odometer_km NUMERIC(14,3), engine_hours NUMERIC(14,3),
 trust VARCHAR(12) NOT NULL, quality VARCHAR(32) NOT NULL, ordering_classification VARCHAR(20) NOT NULL,
 retention_policy VARCHAR(80) NOT NULL, retention_policy_version VARCHAR(40) NOT NULL, retain_until TIMESTAMPTZ,
 safe_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
 CONSTRAINT uq_tracking_position_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_tracking_position_dedupe UNIQUE(tenant_id,dedupe_identity),
 CONSTRAINT fk_tracking_position_device FOREIGN KEY(device_id,tenant_id) REFERENCES tracking_device(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT ck_tracking_lat CHECK(latitude BETWEEN -90 AND 90), CONSTRAINT ck_tracking_lon CHECK(longitude BETWEEN -180 AND 180),
 CONSTRAINT ck_tracking_accuracy CHECK(horizontal_accuracy_meters IS NULL OR horizontal_accuracy_meters>=0),
 CONSTRAINT ck_tracking_speed CHECK(speed_kph IS NULL OR speed_kph BETWEEN 0 AND 400),
 CONSTRAINT ck_tracking_heading CHECK(heading_degrees IS NULL OR (heading_degrees>=0 AND heading_degrees<360)),
 CONSTRAINT ck_tracking_engine CHECK(engine_state IN('ON','OFF','UNKNOWN')),
 CONSTRAINT ck_tracking_trust CHECK(trust IN('TRUSTED','UNTRUSTED','UNKNOWN')),
 CONSTRAINT ck_tracking_order CHECK(ordering_classification IN('IN_ORDER','CLOCK_SKEW','OUT_OF_ORDER','LATE','FUTURE'))
);
CREATE INDEX idx_tracking_vehicle_time ON tracking_position(tenant_id,vehicle_id,source_timestamp DESC,id DESC);
CREATE INDEX idx_tracking_device_time ON tracking_position(tenant_id,device_id,source_timestamp DESC,id DESC);
CREATE UNIQUE INDEX uq_tracking_provider_message ON tracking_position(tenant_id,provider_alias,provider_message_id) WHERE provider_message_id IS NOT NULL;

CREATE TABLE tracking_vehicle_latest (
 tenant_id UUID NOT NULL, vehicle_id UUID NOT NULL, latest_received_position_id UUID NOT NULL,
 latest_trusted_position_id UUID, last_successful_receipt_at TIMESTAMPTZ NOT NULL, policy_version VARCHAR(40) NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,vehicle_id),
 CONSTRAINT fk_tracking_latest_received FOREIGN KEY(latest_received_position_id,tenant_id) REFERENCES tracking_position(id,tenant_id) ON DELETE RESTRICT,
 CONSTRAINT fk_tracking_latest_trusted FOREIGN KEY(latest_trusted_position_id,tenant_id) REFERENCES tracking_position(id,tenant_id) ON DELETE RESTRICT
);

CREATE TABLE tracking_ingest_nonce (
 tenant_id UUID NOT NULL, provider_alias VARCHAR(80) NOT NULL, nonce_hash CHAR(64) NOT NULL,
 used_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(tenant_id,provider_alias,nonce_hash)
);
CREATE INDEX idx_tracking_nonce_expiry ON tracking_ingest_nonce(tenant_id,expires_at);

CREATE TABLE tracking_audit_event (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, actor_id UUID NOT NULL, action VARCHAR(60) NOT NULL,
 target_type VARCHAR(40) NOT NULL, target_id UUID NOT NULL, safe_detail VARCHAR(300), occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_tracking_audit_target ON tracking_audit_event(tenant_id,target_type,target_id,occurred_at DESC);

CREATE OR REPLACE FUNCTION prevent_tracking_position_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'tracking position history is append-only'; END $$;
CREATE TRIGGER trg_tracking_position_immutable BEFORE UPDATE OR DELETE ON tracking_position FOR EACH ROW EXECUTE FUNCTION prevent_tracking_position_mutation();
CREATE OR REPLACE FUNCTION prevent_tracking_assignment_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'tracking association history is append-only'; END IF;
 IF OLD.effective_to IS NOT NULL OR NEW.effective_to IS NULL OR NEW.effective_from<>OLD.effective_from OR NEW.tracking_device_id<>OLD.tracking_device_id OR NEW.vehicle_id<>OLD.vehicle_id OR NEW.tenant_id<>OLD.tenant_id THEN RAISE EXCEPTION 'tracking association history is immutable'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_tracking_assignment_immutable BEFORE UPDATE OR DELETE ON tracking_vehicle_device_assignment FOR EACH ROW EXECUTE FUNCTION prevent_tracking_assignment_mutation();

INSERT INTO app_permission(code,description,active) VALUES
 ('TRACKING_VIEW','View same-Tenant live and last-known Vehicle tracking state',TRUE),
 ('TRACKING_HISTORY_VIEW','View same-Tenant bounded immutable tracking history',TRUE),
 ('TRACKING_DEVICE_MANAGE','Manage same-Tenant Tracking devices and Vehicle association',TRUE)
ON CONFLICT(code) DO UPDATE SET description=EXCLUDED.description,active=TRUE;
