-- V35__freight_insurance_permissions.sql
-- Add permissions for freight cargo insurance management (US-28)

INSERT INTO app_permission (code, description, active) VALUES
    ('CARGO_INSURANCE_VIEW', 'View freight insurance policies, claims and settlement history', TRUE),
    ('CARGO_INSURANCE_MANAGE', 'Manage freight insurance policies, assess claims, execute workflow and record settlements', TRUE);
