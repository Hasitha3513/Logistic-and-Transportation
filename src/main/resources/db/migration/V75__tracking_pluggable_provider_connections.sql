-- CS02: evolve the V74 provider binding into the single runtime provider-connection authority.
-- Existing repository data has only the real FLESPI alias and the controlled FIXTURE alias.
DO $$
BEGIN
 IF EXISTS (
  SELECT 1 FROM tracking_provider_binding
  WHERE upper(provider_alias) NOT IN ('FLESPI','FIXTURE')
 ) THEN
  RAISE EXCEPTION 'V75 cannot infer provider type for an unsupported existing provider alias'
   USING ERRCODE='23514';
 END IF;
 IF EXISTS (
  SELECT 1 FROM tracking_provider_binding
  GROUP BY tenant_id,provider_alias HAVING count(*)>1
 ) THEN
  RAISE EXCEPTION 'V75 found duplicate provider aliases inside a Tenant'
   USING ERRCODE='23505';
 END IF;
END $$;

ALTER TABLE tracking_provider_binding
 ADD COLUMN provider_type VARCHAR(64),
 ADD COLUMN display_name VARCHAR(120),
 ADD COLUMN endpoint_uri VARCHAR(500),
 ADD COLUMN safe_configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
 ADD COLUMN poll_interval_seconds INTEGER NOT NULL DEFAULT 5,
 ADD COLUMN page_size INTEGER NOT NULL DEFAULT 500,
 ADD COLUMN test_status VARCHAR(24) NOT NULL DEFAULT 'NOT_TESTED',
 ADD COLUMN last_tested_at TIMESTAMPTZ,
 ADD COLUMN last_successful_poll_at TIMESTAMPTZ,
 ADD COLUMN last_provider_message_at TIMESTAMPTZ,
 ADD COLUMN last_error_category VARCHAR(40),
 ADD COLUMN next_poll_at TIMESTAMPTZ,
 ADD COLUMN lease_owner VARCHAR(120),
 ADD COLUMN lease_until TIMESTAMPTZ;

UPDATE tracking_provider_binding
 SET provider_type=upper(provider_alias),display_name=provider_alias;

ALTER TABLE tracking_provider_binding
 ALTER COLUMN provider_type SET NOT NULL,
 ALTER COLUMN display_name SET NOT NULL,
 ADD CONSTRAINT uq_tracking_provider_alias UNIQUE(tenant_id,provider_alias),
 ADD CONSTRAINT uq_tracking_provider_display_name UNIQUE(tenant_id,display_name),
 ADD CONSTRAINT ck_tracking_provider_type
  CHECK(provider_type ~ '^[A-Z][A-Z0-9_]{0,63}$'),
 ADD CONSTRAINT ck_tracking_provider_display_name
  CHECK(display_name=btrim(display_name) AND display_name<>''),
 ADD CONSTRAINT ck_tracking_provider_safe_configuration
  CHECK(jsonb_typeof(safe_configuration)='object'
   AND octet_length(safe_configuration::text)<=8192),
 ADD CONSTRAINT ck_tracking_provider_poll_interval
  CHECK(poll_interval_seconds BETWEEN 5 AND 86400),
 ADD CONSTRAINT ck_tracking_provider_page_size
  CHECK(page_size BETWEEN 1 AND 500),
 ADD CONSTRAINT ck_tracking_provider_test_status
  CHECK(test_status IN('NOT_TESTED','PASS','AUTH_FAILED','UNREACHABLE','INVALID_CONFIGURATION')),
 ADD CONSTRAINT ck_tracking_provider_error_category
  CHECK(last_error_category IS NULL
   OR last_error_category ~ '^[A-Z][A-Z0-9_]{0,39}$'),
 ADD CONSTRAINT ck_tracking_provider_lease_pair
  CHECK((lease_owner IS NULL AND lease_until IS NULL)
   OR (lease_owner IS NOT NULL AND lease_until IS NOT NULL));

ALTER TABLE tracking_provider_binding
 DROP CONSTRAINT ck_tracking_provider_binding_lifecycle,
 ADD CONSTRAINT ck_tracking_provider_binding_lifecycle
  CHECK(lifecycle IN('DRAFT','ACTIVE','DISABLED','RETIRED'));

CREATE INDEX idx_tracking_provider_type
 ON tracking_provider_binding(tenant_id,provider_type,lifecycle,id);
CREATE INDEX idx_tracking_provider_due
 ON tracking_provider_binding(next_poll_at,id)
 WHERE lifecycle='ACTIVE' AND next_poll_at IS NOT NULL;

-- V75 reserves the complete device lifecycle vocabulary for CS03 without changing runtime behavior.
ALTER TABLE tracking_device
 DROP CONSTRAINT ck_tracking_device_lifecycle,
 ADD CONSTRAINT ck_tracking_device_lifecycle
  CHECK(lifecycle IN('DRAFT','ACTIVE','DISABLED','RETIRED'));
