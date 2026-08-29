${deliveryStatusConstraintUpgrade}
ALTER TABLE delivery_order ADD CONSTRAINT uk_delivery_order_id_tenant UNIQUE (id, tenant_id);

CREATE TABLE proof_of_delivery (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_order_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'FINALIZED')),
    device_captured_at TIMESTAMPTZ,
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    accuracy_meters NUMERIC(12,3),
    signer_name VARCHAR(200),
    signer_relationship VARCHAR(100),
    accepted_at TIMESTAMPTZ,
    accepted_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uk_pod_tenant_delivery UNIQUE (tenant_id, delivery_order_id),
    CONSTRAINT uk_pod_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_pod_delivery_tenant FOREIGN KEY (delivery_order_id, tenant_id)
        REFERENCES delivery_order(id, tenant_id),
    CONSTRAINT ck_pod_geo_pair CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CONSTRAINT ck_pod_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_pod_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_pod_accuracy CHECK (accuracy_meters IS NULL OR accuracy_meters > 0),
    CONSTRAINT ck_pod_finalization CHECK (
        (status = 'DRAFT' AND accepted_at IS NULL AND accepted_by IS NULL) OR
        (status = 'FINALIZED' AND accepted_at IS NOT NULL AND accepted_by IS NOT NULL)
    )
);

CREATE INDEX idx_pod_tenant_status ON proof_of_delivery(tenant_id, status);

CREATE TABLE pod_evidence (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    proof_of_delivery_id UUID NOT NULL,
    evidence_type VARCHAR(20) NOT NULL CHECK (evidence_type IN ('SIGNATURE', 'PHOTO', 'BARCODE')),
    storage_reference VARCHAR(255),
    barcode_value VARCHAR(64),
    detected_content_type VARCHAR(50),
    content_length BIGINT,
    sha256_checksum VARCHAR(64),
    original_filename VARCHAR(255),
    capture_source VARCHAR(20) NOT NULL CHECK (capture_source IN ('CAMERA', 'FILE', 'SCANNER', 'MANUAL')),
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_pod_evidence_proof_tenant FOREIGN KEY (proof_of_delivery_id, tenant_id)
        REFERENCES proof_of_delivery(id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT ck_pod_evidence_shape CHECK (
        (evidence_type = 'BARCODE' AND barcode_value IS NOT NULL AND storage_reference IS NULL
            AND detected_content_type IS NULL AND content_length IS NULL AND sha256_checksum IS NULL) OR
        (evidence_type IN ('SIGNATURE', 'PHOTO') AND barcode_value IS NULL AND storage_reference IS NOT NULL
            AND detected_content_type IN ('image/png', 'image/jpeg') AND content_length > 0
            AND sha256_checksum ~ '^[0-9a-f]{64}$')
    )
);

CREATE INDEX idx_pod_evidence_tenant_proof ON pod_evidence(tenant_id, proof_of_delivery_id);
${podEvidenceUniquenessIndexes}

INSERT INTO app_permission (code, description, active) VALUES
    ('DELIVERY_POD_CAPTURE', 'Capture and finalize tenant-scoped Proof of Delivery', TRUE),
    ('DELIVERY_POD_VIEW', 'View tenant-scoped Proof of Delivery evidence', TRUE);
