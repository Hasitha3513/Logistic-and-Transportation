CREATE TABLE tenant (
    tenant_id UUID PRIMARY KEY,
    tenant_code VARCHAR(40) NOT NULL UNIQUE,
    tenant_name VARCHAR(200) NOT NULL,
    default_currency VARCHAR(3) NOT NULL,
    default_time_zone VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_tenant_code_nonblank CHECK (BTRIM(tenant_code) <> ''),
    CONSTRAINT chk_tenant_name_nonblank CHECK (BTRIM(tenant_name) <> ''),
    CONSTRAINT chk_tenant_currency_nonblank CHECK (BTRIM(default_currency) <> ''),
    CONSTRAINT chk_tenant_timezone_nonblank CHECK (BTRIM(default_time_zone) <> ''),
    CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE tenant_membership (
    membership_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tenant_membership_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(tenant_id),
    CONSTRAINT fk_tenant_membership_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT uq_tenant_membership_user UNIQUE (user_id),
    CONSTRAINT chk_tenant_membership_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_tenant_membership_tenant ON tenant_membership(tenant_id);

INSERT INTO tenant (
    tenant_id, tenant_code, tenant_name, default_currency, default_time_zone,
    status, created_at, created_by, updated_at, updated_by, version
) VALUES (
    '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a',
    'CLTS-LK',
    'Ceylon Logistics & Transport Solutions (Pvt) Ltd',
    'LKR',
    'Asia/Colombo',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    'TENANT-FOUNDATION-IMPLEMENTATION-001',
    CURRENT_TIMESTAMP,
    'TENANT-FOUNDATION-IMPLEMENTATION-001',
    0
);
