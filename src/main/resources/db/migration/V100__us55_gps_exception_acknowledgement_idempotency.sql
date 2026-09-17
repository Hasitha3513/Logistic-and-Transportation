CREATE TABLE tracking_gps_exception_acknowledgement_command (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL,
 idempotency_key VARCHAR(160) NOT NULL,
 request_fingerprint CHAR(64) NOT NULL,
 actor_id UUID NOT NULL,
 episode_id UUID NOT NULL,
 expected_version BIGINT NOT NULL,
 response_snapshot JSONB NOT NULL,
 completed_at TIMESTAMPTZ NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT uq_tracking_gps_exception_ack_command UNIQUE(tenant_id,idempotency_key),
 CONSTRAINT fk_tracking_gps_exception_ack_episode FOREIGN KEY(tenant_id,episode_id)
  REFERENCES tracking_gps_exception_episode(tenant_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_tracking_gps_exception_ack_key CHECK(char_length(idempotency_key) BETWEEN 16 AND 160),
 CONSTRAINT ck_tracking_gps_exception_ack_fingerprint CHECK(request_fingerprint ~ '^[0-9a-f]{64}$'),
 CONSTRAINT ck_tracking_gps_exception_ack_version CHECK(expected_version>=0),
 CONSTRAINT ck_tracking_gps_exception_ack_response CHECK(jsonb_typeof(response_snapshot)='object')
);

COMMENT ON TABLE tracking_gps_exception_acknowledgement_command IS
 'Immutable successful acknowledgement replay evidence; retained with its GPS exception episode';
