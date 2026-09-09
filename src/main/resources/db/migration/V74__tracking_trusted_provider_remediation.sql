CREATE TABLE tracking_provider_binding (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 provider_key_id VARCHAR(160) NOT NULL,
 provider_alias VARCHAR(80) NOT NULL,
 credential_reference VARCHAR(160) NOT NULL,
 lifecycle VARCHAR(16) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 created_by UUID NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 updated_by UUID NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_tracking_provider_binding_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT uq_tracking_provider_key_id UNIQUE(provider_key_id),
 CONSTRAINT ck_tracking_provider_binding_lifecycle CHECK(lifecycle IN('ACTIVE','DISABLED'))
);
CREATE INDEX idx_tracking_provider_binding_tenant
 ON tracking_provider_binding(tenant_id,lifecycle,provider_alias,id);

CREATE TABLE tracking_provider_ingest_nonce (
 tenant_id UUID NOT NULL,
 provider_binding_id UUID NOT NULL,
 nonce_hash CHAR(64) NOT NULL,
 used_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(provider_binding_id,nonce_hash),
 CONSTRAINT fk_tracking_provider_nonce_binding
  FOREIGN KEY(provider_binding_id,tenant_id)
  REFERENCES tracking_provider_binding(id,tenant_id) ON DELETE RESTRICT
);
CREATE INDEX idx_tracking_provider_nonce_expiry
 ON tracking_provider_ingest_nonce(tenant_id,expires_at,provider_binding_id);

CREATE TABLE tracking_retention_policy (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 retention_duration_seconds BIGINT NOT NULL,
 policy_version VARCHAR(40) NOT NULL,
 effective_at TIMESTAMPTZ NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 created_by UUID NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 updated_by UUID NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_tracking_retention_policy_tenant UNIQUE(tenant_id),
 CONSTRAINT uq_tracking_retention_policy_tenant_id UNIQUE(tenant_id,id),
 CONSTRAINT ck_tracking_retention_duration_positive CHECK(retention_duration_seconds>0)
);
CREATE INDEX idx_tracking_retention_policy_effective
 ON tracking_retention_policy(tenant_id,effective_at DESC,id);

CREATE OR REPLACE FUNCTION prevent_tracking_provider_tenant_reassignment() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.tenant_id<>OLD.tenant_id OR NEW.provider_key_id<>OLD.provider_key_id THEN
  RAISE EXCEPTION 'tracking provider binding authority is immutable';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_tracking_provider_authority_immutable
 BEFORE UPDATE ON tracking_provider_binding
 FOR EACH ROW EXECUTE FUNCTION prevent_tracking_provider_tenant_reassignment();
