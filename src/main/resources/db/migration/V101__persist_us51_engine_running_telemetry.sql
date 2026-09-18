-- US-51 CS02: canonical V3 engine semantics and effective-dated capability vocabulary.
ALTER TABLE tracking_position_history
 DROP CONSTRAINT ck_tracking_history_event_version,
 DROP CONSTRAINT ck_tracking_history_v1_has_no_v2_signals,
 ADD CONSTRAINT ck_tracking_history_event_version CHECK (event_version IN (1, 2, 3)),
 ADD CONSTRAINT ck_tracking_history_v1_has_no_v2_signals
  CHECK (event_version IN (2, 3) OR
   (tamper_state IS NULL AND battery_level_percent IS NULL
    AND battery_voltage_volts IS NULL AND external_power_state IS NULL
    AND battery_charging_state IS NULL)),
 ADD COLUMN ignition_state VARCHAR(16),
 ADD COLUMN engine_running_state VARCHAR(16),
 ADD COLUMN engine_running_source VARCHAR(40),
 ADD CONSTRAINT ck_tracking_history_ignition_state
  CHECK (ignition_state IS NULL OR ignition_state IN ('ON','OFF','UNKNOWN')),
 ADD CONSTRAINT ck_tracking_history_engine_running_state
  CHECK (engine_running_state IS NULL OR
   engine_running_state IN ('RUNNING','NOT_RUNNING','UNKNOWN')),
 ADD CONSTRAINT ck_tracking_history_engine_running_source
  CHECK (engine_running_source IS NULL OR engine_running_source IN
   ('DEVICE_NATIVE_CAN','DEVICE_NATIVE_RPM','DEVICE_NATIVE_STATUS',
    'PROVIDER_VERIFIED_DERIVATION')),
 ADD CONSTRAINT ck_tracking_history_engine_running_pair
  CHECK ((engine_running_state IS NULL) = (engine_running_source IS NULL)),
 ADD CONSTRAINT ck_tracking_history_v3_ignition_consistency
  CHECK (ignition_state IS NULL OR ignition_state = engine_state),
 ADD CONSTRAINT ck_tracking_history_pre_v3_has_no_v3_signals
  CHECK (event_version = 3 OR
   (ignition_state IS NULL AND engine_running_state IS NULL
    AND engine_running_source IS NULL));

ALTER TABLE tracking_device_telemetry_capability
 DROP CONSTRAINT ck_tracking_device_capability_name,
 ADD CONSTRAINT ck_tracking_device_capability_name CHECK (capability IN
  ('POSITION','SPEED','ACCURACY','HEADING','IGNITION','ENGINE_RUNNING','TAMPER',
   'BATTERY_LEVEL','BATTERY_VOLTAGE','EXTERNAL_POWER','BATTERY_CHARGING'));
