-- CS03: runtime authority for Tracking device-to-provider connection identity.
DO $$
DECLARE
 unmatched_count BIGINT;
 ambiguous_count BIGINT;
BEGIN
 SELECT count(*) INTO unmatched_count
 FROM tracking_device device
 WHERE NOT EXISTS (
  SELECT 1 FROM tracking_provider_binding provider
  WHERE provider.tenant_id=device.tenant_id
    AND provider.provider_alias=device.provider_alias
 );
 SELECT count(*) INTO ambiguous_count
 FROM tracking_device device
 WHERE 1<(
  SELECT count(*) FROM tracking_provider_binding provider
  WHERE provider.tenant_id=device.tenant_id
    AND provider.provider_alias=device.provider_alias
 );
 IF unmatched_count>0 OR ambiguous_count>0 THEN
  RAISE EXCEPTION 'V76 device-provider backfill is not deterministic (unmatched %, ambiguous %)',
   unmatched_count,ambiguous_count USING ERRCODE='23514';
 END IF;
END $$;

CREATE TABLE tracking_device_provider_binding (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 tracking_device_id UUID NOT NULL,
 provider_binding_id UUID NOT NULL,
 external_device_reference VARCHAR(160) NOT NULL,
 safe_configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
 lifecycle VARCHAR(16) NOT NULL,
 watermark_source_timestamp TIMESTAMPTZ,
 watermark_message_identity VARCHAR(160),
 next_poll_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 created_by UUID NOT NULL,
 updated_by UUID NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_tracking_device_provider_binding_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_tracking_device_provider_external
  UNIQUE(tenant_id,provider_binding_id,external_device_reference),
 CONSTRAINT fk_tracking_device_provider_device
  FOREIGN KEY(tenant_id,tracking_device_id)
  REFERENCES tracking_device(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT fk_tracking_device_provider_connection
  FOREIGN KEY(tenant_id,provider_binding_id)
  REFERENCES tracking_provider_binding(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_tracking_device_provider_external
  CHECK(external_device_reference=btrim(external_device_reference)
   AND external_device_reference<>''),
 CONSTRAINT ck_tracking_device_provider_safe_configuration
  CHECK(jsonb_typeof(safe_configuration)='object'
   AND octet_length(safe_configuration::text)<=4096),
 CONSTRAINT ck_tracking_device_provider_lifecycle
  CHECK(lifecycle IN('DRAFT','ACTIVE','DISABLED','RETIRED')),
 CONSTRAINT ck_tracking_device_provider_message_identity
  CHECK(watermark_message_identity IS NULL
   OR (watermark_message_identity=btrim(watermark_message_identity)
    AND watermark_message_identity<>'')),
 CONSTRAINT ck_tracking_device_provider_version CHECK(version>=0)
);

CREATE UNIQUE INDEX uq_tracking_device_provider_active_device
 ON tracking_device_provider_binding(tenant_id,tracking_device_id)
 WHERE lifecycle='ACTIVE';
CREATE INDEX idx_tracking_device_provider_connection_list
 ON tracking_device_provider_binding(tenant_id,provider_binding_id,lifecycle,id);
CREATE INDEX idx_tracking_device_provider_lifecycle
 ON tracking_device_provider_binding(tenant_id,lifecycle,id);
CREATE INDEX idx_tracking_device_provider_due
 ON tracking_device_provider_binding(next_poll_at,id)
 WHERE lifecycle='ACTIVE' AND next_poll_at IS NOT NULL;

INSERT INTO tracking_device_provider_binding(
 id,tenant_id,tracking_device_id,provider_binding_id,external_device_reference,
 safe_configuration,lifecycle,watermark_source_timestamp,watermark_message_identity,next_poll_at,
 created_at,updated_at,created_by,updated_by,version)
SELECT gen_random_uuid(),device.tenant_id,device.id,provider.id,device.external_device_reference,
 '{}'::jsonb,
 CASE WHEN device.lifecycle='ACTIVE' AND provider.lifecycle='ACTIVE' THEN 'ACTIVE' ELSE 'DISABLED' END,
 NULL,NULL,NULL,device.created_at,device.updated_at,device.registered_by,device.registered_by,0
FROM tracking_device device
JOIN tracking_provider_binding provider
 ON provider.tenant_id=device.tenant_id
AND provider.provider_alias=device.provider_alias;
