-- V36__freight_insurance_tables.sql
-- Tables for Freight Cargo Insurance, Claims, and Settlements (US-28)

CREATE SEQUENCE freight_policy_number_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE freight_claim_number_sequence  START WITH 1 INCREMENT BY 1;

CREATE TABLE freight_insurance_policy (
    id                  UUID            PRIMARY KEY,
    policy_number       VARCHAR(60)     NOT NULL UNIQUE,
    freight_order_id    UUID            NOT NULL,
    cargo_manifest_id   UUID,
    insurance_provider  VARCHAR(200)    NOT NULL,
    policy_type         VARCHAR(60)     NOT NULL,
    coverage_amount     NUMERIC(19,4)   NOT NULL,
    premium_amount      NUMERIC(19,4)   NOT NULL,
    currency            VARCHAR(10)     NOT NULL,
    valid_from          TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until         TIMESTAMP WITH TIME ZONE NOT NULL,
    status              VARCHAR(30)     NOT NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(128)    NOT NULL,
    updated_by          VARCHAR(128)    NOT NULL,
    CONSTRAINT fk_insurance_freight_order FOREIGN KEY (freight_order_id) REFERENCES freight_order(id),
    CONSTRAINT chk_policy_coverage_positive CHECK (coverage_amount > 0),
    CONSTRAINT chk_policy_premium_non_neg   CHECK (premium_amount >= 0)
);

CREATE TABLE freight_insurance_claim (
    id                  UUID            PRIMARY KEY,
    claim_number        VARCHAR(60)     NOT NULL UNIQUE,
    policy_id           UUID            NOT NULL,
    freight_order_id    UUID            NOT NULL,
    incident_reference  VARCHAR(120),
    damage_description  VARCHAR(2000)   NOT NULL,
    claimed_amount      NUMERIC(19,4)   NOT NULL,
    assessed_amount     NUMERIC(19,4),
    assessment_notes    VARCHAR(2000),
    assessed_by         VARCHAR(128),
    assessed_at         TIMESTAMP WITH TIME ZONE,
    status              VARCHAR(30)     NOT NULL,
    resolution_reason   VARCHAR(2000),
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(128)    NOT NULL,
    updated_by          VARCHAR(128)    NOT NULL,
    CONSTRAINT fk_claim_policy        FOREIGN KEY (policy_id)        REFERENCES freight_insurance_policy(id),
    CONSTRAINT fk_claim_freight_order FOREIGN KEY (freight_order_id) REFERENCES freight_order(id),
    CONSTRAINT chk_claim_amount_positive CHECK (claimed_amount > 0)
);

CREATE TABLE freight_insurance_settlement (
    id                      UUID            PRIMARY KEY,
    claim_id                UUID            NOT NULL,
    settlement_reference    VARCHAR(120)    NOT NULL,
    amount                  NUMERIC(19,4)   NOT NULL,
    currency                VARCHAR(10)     NOT NULL,
    notes                   VARCHAR(1000),
    settled_by              VARCHAR(128)    NOT NULL,
    settled_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_settlement_claim  FOREIGN KEY (claim_id) REFERENCES freight_insurance_claim(id) ON DELETE CASCADE,
    CONSTRAINT chk_settlement_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_insurance_policy_order ON freight_insurance_policy(freight_order_id);
CREATE INDEX idx_insurance_claim_policy ON freight_insurance_claim(policy_id);
CREATE INDEX idx_insurance_claim_order  ON freight_insurance_claim(freight_order_id);
CREATE INDEX idx_settlement_claim       ON freight_insurance_settlement(claim_id);
