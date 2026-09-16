-- US-55 CS03: immutable canonical V2 evidence and effective-dated device capabilities.
ALTER TABLE tracking_position_history
 DROP CONSTRAINT ck_tracking_history_event_version,
 ADD CONSTRAINT ck_tracking_history_event_version CHECK (event_version IN (1, 2)),
 ADD COLUMN tamper_state VARCHAR(16),
 ADD COLUMN battery_level_percent NUMERIC,
 ADD COLUMN battery_voltage_volts NUMERIC,
 ADD COLUMN external_power_state VARCHAR(16),
 ADD COLUMN battery_charging_state VARCHAR(20),
 ADD CONSTRAINT ck_tracking_history_tamper_state
  CHECK (tamper_state IS NULL OR tamper_state IN ('DETECTED','CLEAR','UNKNOWN')),
 ADD CONSTRAINT ck_tracking_history_battery_level
  CHECK (battery_level_percent IS NULL OR
   (battery_level_percent BETWEEN 0 AND 100 AND scale(battery_level_percent) <= 3)),
 ADD CONSTRAINT ck_tracking_history_battery_voltage
  CHECK (battery_voltage_volts IS NULL OR
   (battery_voltage_volts BETWEEN 0 AND 1000 AND scale(battery_voltage_volts) <= 6)),
 ADD CONSTRAINT ck_tracking_history_external_power
  CHECK (external_power_state IS NULL OR
   external_power_state IN ('CONNECTED','DISCONNECTED','UNKNOWN')),
 ADD CONSTRAINT ck_tracking_history_battery_charging
  CHECK (battery_charging_state IS NULL OR
   battery_charging_state IN ('CHARGING','NOT_CHARGING','UNKNOWN')),
 ADD CONSTRAINT ck_tracking_history_v1_has_no_v2_signals
  CHECK (event_version = 2 OR
   (tamper_state IS NULL AND battery_level_percent IS NULL
    AND battery_voltage_volts IS NULL AND external_power_state IS NULL
    AND battery_charging_state IS NULL));

CREATE OR REPLACE FUNCTION prevent_tracking_position_history_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'tracking position history is append-only';
END $$;

CREATE TRIGGER trg_tracking_position_history_immutable
 BEFORE UPDATE OR DELETE ON tracking_position_history
 FOR EACH ROW EXECUTE FUNCTION prevent_tracking_position_history_mutation();

CREATE TABLE tracking_device_telemetry_capability (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 tracking_device_id UUID NOT NULL,
 capability VARCHAR(32) NOT NULL,
 capability_state VARCHAR(16) NOT NULL,
 effective_from TIMESTAMPTZ NOT NULL,
 effective_to TIMESTAMPTZ,
 recorded_at TIMESTAMPTZ NOT NULL,
 recorded_by UUID NOT NULL,
 CONSTRAINT uq_tracking_device_capability_tenant_id UNIQUE (tenant_id,id),
 CONSTRAINT fk_tracking_device_capability_device
  FOREIGN KEY (tenant_id,tracking_device_id)
  REFERENCES tracking_device(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_tracking_device_capability_name CHECK (capability IN
  ('POSITION','SPEED','ACCURACY','HEADING','IGNITION','TAMPER','BATTERY_LEVEL',
   'BATTERY_VOLTAGE','EXTERNAL_POWER','BATTERY_CHARGING')),
 CONSTRAINT ck_tracking_device_capability_state CHECK
  (capability_state IN ('SUPPORTED','UNSUPPORTED','UNKNOWN')),
 CONSTRAINT ck_tracking_device_capability_interval CHECK
  (effective_to IS NULL OR effective_to > effective_from)
);

CREATE UNIQUE INDEX uq_tracking_device_capability_active
 ON tracking_device_telemetry_capability(tenant_id,tracking_device_id,capability)
 WHERE effective_to IS NULL;
CREATE INDEX idx_tracking_device_capability_effective
 ON tracking_device_telemetry_capability(
  tenant_id,tracking_device_id,capability,effective_from DESC,id DESC);

CREATE OR REPLACE FUNCTION reject_tracking_device_capability_overlap()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS (
  SELECT 1 FROM tracking_device_telemetry_capability existing
  WHERE existing.tenant_id=NEW.tenant_id
    AND existing.tracking_device_id=NEW.tracking_device_id
    AND existing.capability=NEW.capability
    AND existing.id<>NEW.id
    AND tstzrange(existing.effective_from,existing.effective_to,'[)') &&
        tstzrange(NEW.effective_from,NEW.effective_to,'[)')
 ) THEN
  RAISE EXCEPTION 'tracking device capability interval overlaps existing history'
   USING ERRCODE='23P01';
 END IF;
 RETURN NEW;
END $$;

CREATE TRIGGER trg_tracking_device_capability_no_overlap
 BEFORE INSERT OR UPDATE OF effective_to
 ON tracking_device_telemetry_capability
 FOR EACH ROW EXECUTE FUNCTION reject_tracking_device_capability_overlap();

CREATE OR REPLACE FUNCTION prevent_tracking_device_capability_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN
  RAISE EXCEPTION 'tracking device capability history is append-only';
 END IF;
 IF OLD.effective_to IS NOT NULL OR NEW.effective_to IS NULL
    OR NEW.tenant_id<>OLD.tenant_id
    OR NEW.tracking_device_id<>OLD.tracking_device_id
    OR NEW.capability<>OLD.capability
    OR NEW.capability_state<>OLD.capability_state
    OR NEW.effective_from<>OLD.effective_from
    OR NEW.recorded_at<>OLD.recorded_at
    OR NEW.recorded_by<>OLD.recorded_by THEN
  RAISE EXCEPTION 'tracking device capability history is immutable';
 END IF;
 RETURN NEW;
END $$;

CREATE TRIGGER trg_tracking_device_capability_immutable
 BEFORE UPDATE OR DELETE ON tracking_device_telemetry_capability
 FOR EACH ROW EXECUTE FUNCTION prevent_tracking_device_capability_mutation();
