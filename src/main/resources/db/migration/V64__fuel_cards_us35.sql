CREATE TABLE fuel_card (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    alias VARCHAR(100) NOT NULL,
    provider_card_reference VARCHAR(255) NOT NULL,
    provider_reference_hash CHAR(64) NOT NULL,
    masked_identifier VARCHAR(32) NOT NULL,
    last_four CHAR(4),
    expiry_month SMALLINT NOT NULL CHECK (expiry_month BETWEEN 1 AND 12),
    expiry_year SMALLINT NOT NULL CHECK (expiry_year BETWEEN 2000 AND 9999),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUSPENDED','BLOCKED','EXPIRED','CANCELLED')),
    provider_sync_status VARCHAR(20) NOT NULL DEFAULT 'NOT_CONFIGURED' CHECK (provider_sync_status = 'NOT_CONFIGURED'),
    version BIGINT NOT NULL DEFAULT 0,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, id),
    UNIQUE (tenant_id, provider_id, provider_card_reference)
);
CREATE INDEX idx_fuel_card_tenant_status ON fuel_card (tenant_id, status, created_at DESC);
CREATE INDEX idx_fuel_card_tenant_provider_hash ON fuel_card (tenant_id, provider_id, provider_reference_hash);
CREATE INDEX idx_fuel_card_tenant_expiry ON fuel_card (tenant_id, expiry_year, expiry_month);

CREATE TABLE fuel_card_binding_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    card_id UUID NOT NULL,
    binding_type VARCHAR(10) NOT NULL CHECK (binding_type IN ('VEHICLE','DRIVER')),
    binding_id UUID NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    reason VARCHAR(500) NOT NULL,
    changed_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, card_id) REFERENCES fuel_card (tenant_id, id)
);
CREATE UNIQUE INDEX uq_fuel_card_one_active_binding ON fuel_card_binding_history (tenant_id, card_id) WHERE effective_to IS NULL;
CREATE INDEX idx_fuel_card_binding_target ON fuel_card_binding_history (tenant_id, binding_type, binding_id, effective_to);

CREATE TABLE fuel_card_restriction (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    card_id UUID NOT NULL,
    currency CHAR(3) NOT NULL,
    max_transaction_amount NUMERIC(19,2) NOT NULL CHECK (max_transaction_amount > 0),
    max_daily_amount NUMERIC(19,2) NOT NULL CHECK (max_daily_amount > 0),
    max_monthly_amount NUMERIC(19,2) NOT NULL CHECK (max_monthly_amount > 0),
    max_daily_litres NUMERIC(19,4) NOT NULL CHECK (max_daily_litres > 0),
    allowed_fuel_types TEXT NOT NULL,
    allowed_station_references TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    changed_by UUID NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, card_id),
    FOREIGN KEY (tenant_id, card_id) REFERENCES fuel_card (tenant_id, id)
);

CREATE TABLE fuel_card_import_batch (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    provider_batch_id VARCHAR(120) NOT NULL,
    file_hash CHAR(64) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    transaction_count INTEGER NOT NULL CHECK (transaction_count BETWEEN 1 AND 1000),
    imported_count INTEGER NOT NULL,
    review_count INTEGER NOT NULL,
    imported_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, id),
    UNIQUE (tenant_id, provider_id, provider_batch_id),
    UNIQUE (tenant_id, provider_id, file_hash)
);
CREATE INDEX idx_fuel_card_batch_created ON fuel_card_import_batch (tenant_id, created_at DESC);

CREATE TABLE fuel_card_transaction (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    batch_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    card_id UUID NOT NULL,
    provider_transaction_id VARCHAR(120) NOT NULL,
    canonical_hash CHAR(64) NOT NULL,
    transaction_kind VARCHAR(10) NOT NULL CHECK (transaction_kind IN ('PURCHASE','REVERSAL')),
    original_provider_transaction_id VARCHAR(120),
    transaction_timestamp TIMESTAMPTZ NOT NULL,
    posted_timestamp TIMESTAMPTZ,
    station_reference VARCHAR(120),
    fuel_type VARCHAR(40) NOT NULL,
    quantity_litres NUMERIC(19,4) NOT NULL CHECK (quantity_litres > 0),
    unit_price NUMERIC(19,4) NOT NULL CHECK (unit_price > 0),
    total_amount NUMERIC(19,2) NOT NULL CHECK (total_amount > 0),
    currency CHAR(3) NOT NULL,
    provider_vehicle_reference VARCHAR(120),
    provider_driver_reference VARCHAR(120),
    trip_id UUID,
    provider_status VARCHAR(10) NOT NULL CHECK (provider_status IN ('POSTED','REVERSED')),
    local_status VARCHAR(20) NOT NULL CHECK (local_status IN ('IMPORTED','REVIEW_REQUIRED','RECONCILED','REJECTED','REVERSED')),
    reconciled_purchase_id UUID,
    imported_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, id),
    UNIQUE (tenant_id, provider_id, provider_transaction_id),
    FOREIGN KEY (tenant_id, batch_id) REFERENCES fuel_card_import_batch (tenant_id, id),
    FOREIGN KEY (tenant_id, card_id) REFERENCES fuel_card (tenant_id, id)
);
CREATE INDEX idx_fuel_card_txn_search ON fuel_card_transaction (tenant_id, card_id, transaction_timestamp DESC, local_status);
CREATE UNIQUE INDEX uq_fuel_card_active_purchase_reconciliation ON fuel_card_transaction (tenant_id, reconciled_purchase_id)
    WHERE reconciled_purchase_id IS NOT NULL AND transaction_kind = 'PURCHASE' AND local_status = 'RECONCILED';

CREATE TABLE fuel_card_reconciliation_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    action VARCHAR(24) NOT NULL CHECK (action IN ('MATCH','UNMATCH','REJECT','REVERSAL_DISPOSITION')),
    purchase_id UUID,
    reason VARCHAR(500) NOT NULL,
    actor_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (tenant_id, transaction_id) REFERENCES fuel_card_transaction (tenant_id, id)
);
CREATE INDEX idx_fuel_card_recon_history ON fuel_card_reconciliation_history (tenant_id, transaction_id, created_at DESC);

CREATE TABLE fuel_card_transaction_indicator (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL CHECK (code IN ('BINDING_MISMATCH','FUEL_TYPE_NOT_ALLOWED','STATION_NOT_ALLOWED','LIMIT_EXCEEDED','CARD_INACTIVE','TRANSACTION_INTEGRITY_CONFLICT','REVERSAL_REVIEW_REQUIRED')),
    detail_code VARCHAR(60),
    created_at TIMESTAMPTZ NOT NULL,
    acknowledged_by UUID,
    acknowledged_at TIMESTAMPTZ,
    UNIQUE (tenant_id, transaction_id, code, detail_code),
    FOREIGN KEY (tenant_id, transaction_id) REFERENCES fuel_card_transaction (tenant_id, id)
);
CREATE INDEX idx_fuel_card_indicator_review ON fuel_card_transaction_indicator (tenant_id, code, created_at DESC);

CREATE TABLE fuel_card_audit_event (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    card_id UUID,
    transaction_id UUID,
    action VARCHAR(50) NOT NULL,
    result VARCHAR(30) NOT NULL,
    reason_code VARCHAR(80),
    before_hash CHAR(64),
    after_hash CHAR(64),
    actor_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_fuel_card_audit_card ON fuel_card_audit_event (tenant_id, card_id, created_at DESC);

INSERT INTO app_permission (code, description, active)
SELECT permission_name, description, TRUE
FROM (VALUES
    ('FUEL_CARD_VIEW', 'View masked fuel cards, imports, transactions, and indicators'),
    ('FUEL_CARD_MANAGE', 'Manage local fuel-card lifecycle, bindings, and restrictions'),
    ('FUEL_CARD_BLOCK', 'Block fuel cards locally'),
    ('FUEL_CARD_IMPORT', 'Import canonical fuel-card transaction batches'),
    ('FUEL_CARD_RECONCILE', 'Reconcile imported fuel-card transactions')
) AS required_permissions(permission_name, description)
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    active = EXCLUDED.active;
