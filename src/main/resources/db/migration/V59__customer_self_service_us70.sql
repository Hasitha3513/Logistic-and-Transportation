-- US-70 customer self-service access and customer-originated submissions.

CREATE TABLE delivery_self_service_access (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    recipient_contact_hash CHAR(64) NOT NULL,
    contact_hash_key_version VARCHAR(32) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    allowed_actions VARCHAR[] NOT NULL,
    issuance_idempotency_key VARCHAR(128) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    use_count BIGINT NOT NULL DEFAULT 0,
    revocation_reason VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    CONSTRAINT uk_self_service_token_hash UNIQUE (token_hash),
    CONSTRAINT uk_self_service_access_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uk_self_service_issuance UNIQUE (tenant_id, issuance_idempotency_key),
    CONSTRAINT chk_self_service_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_self_service_contact_hash CHECK (recipient_contact_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_self_service_expiry CHECK (expires_at > issued_at),
    CONSTRAINT chk_self_service_use_count CHECK (use_count >= 0),
    CONSTRAINT chk_self_service_actions CHECK (cardinality(allowed_actions) > 0),
    CONSTRAINT fk_self_service_delivery_tenant FOREIGN KEY (delivery_order_id, tenant_id)
        REFERENCES delivery_order (id, tenant_id) ON DELETE RESTRICT
);

CREATE INDEX idx_self_service_tenant_delivery_customer
    ON delivery_self_service_access (tenant_id, delivery_order_id, customer_id);
CREATE INDEX idx_self_service_active_expiry
    ON delivery_self_service_access (tenant_id, expires_at) WHERE revoked_at IS NULL;
CREATE INDEX idx_self_service_revoked_expiry
    ON delivery_self_service_access (tenant_id, revoked_at, expires_at);

CREATE TABLE delivery_customer_submission (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    access_id UUID NOT NULL,
    submission_type VARCHAR(32) NOT NULL,
    category VARCHAR(64),
    description VARCHAR(1000),
    rating SMALLINT,
    preferred_start_at TIMESTAMP WITH TIME ZONE,
    preferred_end_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    operator_outcome VARCHAR(64),
    operator_outcome_at TIMESTAMP WITH TIME ZONE,
    operator_outcome_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_customer_submission_type CHECK
        (submission_type IN ('DELIVERY_PREFERENCE', 'REDELIVERY_REQUEST', 'ISSUE', 'FEEDBACK')),
    CONSTRAINT chk_customer_submission_status CHECK
        (status IN ('SUBMITTED', 'RECORDED', 'ACCEPTED', 'DECLINED', 'SUPERSEDED')),
    CONSTRAINT chk_customer_submission_category CHECK
        (category IS NULL OR category IN ('DELIVERY_TIMING', 'ACCESS_OR_ADDRESS_CLARIFICATION',
         'DELIVERY_CONDITION', 'DELIVERY_SERVICE', 'OTHER')),
    CONSTRAINT chk_customer_submission_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    CONSTRAINT chk_customer_submission_window_pair CHECK
        ((preferred_start_at IS NULL) = (preferred_end_at IS NULL)),
    CONSTRAINT chk_customer_submission_window_order CHECK
        (preferred_start_at IS NULL OR preferred_end_at > preferred_start_at),
    CONSTRAINT chk_customer_submission_fields CHECK (
        (submission_type = 'ISSUE' AND category IS NOT NULL AND char_length(description) BETWEEN 10 AND 1000
            AND rating IS NULL AND preferred_start_at IS NULL)
        OR (submission_type = 'FEEDBACK' AND category IS NULL AND rating IS NOT NULL
            AND (description IS NULL OR char_length(description) BETWEEN 1 AND 1000)
            AND preferred_start_at IS NULL)
        OR (submission_type IN ('DELIVERY_PREFERENCE', 'REDELIVERY_REQUEST') AND category IS NULL AND rating IS NULL
            AND (description IS NULL OR char_length(description) BETWEEN 1 AND 1000))
    ),
    CONSTRAINT chk_customer_submission_request_hash CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_customer_submission_idempotency_length CHECK
        (char_length(idempotency_key) BETWEEN 16 AND 128),
    CONSTRAINT uk_customer_submission_idempotency
        UNIQUE (tenant_id, access_id, submission_type, idempotency_key),
    CONSTRAINT fk_customer_submission_delivery_tenant FOREIGN KEY (delivery_order_id, tenant_id)
        REFERENCES delivery_order (id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_customer_submission_access_tenant FOREIGN KEY (access_id, tenant_id)
        REFERENCES delivery_self_service_access (id, tenant_id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uk_customer_feedback_once
    ON delivery_customer_submission (tenant_id, delivery_order_id, customer_id)
    WHERE submission_type = 'FEEDBACK' AND status <> 'SUPERSEDED';
CREATE INDEX idx_customer_submission_delivery
    ON delivery_customer_submission (tenant_id, delivery_order_id, customer_id, submission_type, created_at DESC);

-- Persist only a controlled placeholder. The worker substitutes the transient URL at provider-send time.
UPDATE notification_template
SET body = body || ' Track or manage this delivery: [[SELF_SERVICE_LINK]]', updated_at = CURRENT_TIMESTAMP
WHERE event_type IN ('DELIVERY_OUT_FOR_DELIVERY', 'DELIVERY_ETA_RISK_CHANGED', 'DELIVERY_COMPLETED',
                     'DELIVERY_FAILED_ATTEMPT_RECORDED', 'DELIVERY_REDELIVERY_SCHEDULED')
  AND channel IN ('EMAIL', 'SMS')
  AND body NOT LIKE '%[[SELF_SERVICE_LINK]]%';
