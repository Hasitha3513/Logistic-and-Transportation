-- V41__cargo_exception_tables.sql
-- Tables for Cargo Exception lifecycle and audit history (US-30)

CREATE TABLE cargo_exception (
    id                  UUID            PRIMARY KEY,
    exception_number    VARCHAR(32)     NOT NULL UNIQUE,
    exception_type      VARCHAR(40)     NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    severity            VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    freight_order_id    UUID            NOT NULL,
    manifest_id         UUID,
    manifest_item_id    UUID,
    description         VARCHAR(2000)   NOT NULL,
    impact              VARCHAR(2000),
    restriction         VARCHAR(1000),
    corrective_action   VARCHAR(2000),
    resolution          VARCHAR(2000),
    resolved_at         TIMESTAMP WITH TIME ZONE,
    resolved_by         VARCHAR(128),
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(128)    NOT NULL,
    updated_by          VARCHAR(128)    NOT NULL,
    CONSTRAINT fk_cargo_exception_freight_order FOREIGN KEY (freight_order_id) REFERENCES freight_order(id),
    CONSTRAINT chk_cargo_exception_type CHECK (exception_type IN (
        'DAMAGE', 'PARTIAL_SHIPMENT', 'WEIGHT_DISCREPANCY',
        'HAZARDOUS_MATERIAL', 'UNMANIFESTED_CARGO', 'SEAL_TAMPERING'
    )),
    CONSTRAINT chk_cargo_exception_status CHECK (status IN (
        'OPEN', 'HELD', 'ESCALATED', 'RESOLVED', 'REJECTED'
    )),
    CONSTRAINT chk_cargo_exception_severity CHECK (severity IN (
        'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    ))
);

CREATE TABLE cargo_exception_history (
    id              UUID                     PRIMARY KEY,
    exception_id    UUID                     NOT NULL,
    action          VARCHAR(60)              NOT NULL,
    actor           VARCHAR(128)             NOT NULL,
    occurred_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    reason          VARCHAR(2000),
    details         VARCHAR(2000),
    CONSTRAINT fk_exception_history_exception FOREIGN KEY (exception_id)
        REFERENCES cargo_exception(id) ON DELETE CASCADE
);

CREATE INDEX idx_cargo_exception_freight_order ON cargo_exception(freight_order_id);
CREATE INDEX idx_cargo_exception_manifest      ON cargo_exception(manifest_id);
CREATE INDEX idx_cargo_exception_type          ON cargo_exception(exception_type);
CREATE INDEX idx_cargo_exception_status        ON cargo_exception(status);
CREATE INDEX idx_cargo_exception_created_at    ON cargo_exception(created_at DESC);
CREATE INDEX idx_cargo_exception_history_exc   ON cargo_exception_history(exception_id);
