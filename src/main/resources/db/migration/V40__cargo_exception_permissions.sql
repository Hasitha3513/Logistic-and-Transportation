-- V40__cargo_exception_permissions.sql
-- Adds RBAC permissions for Cargo Exception (US-30)

INSERT INTO app_permission (code, description, active) VALUES
    ('CARGO_EXCEPTION_VIEW',   'View cargo exceptions and retained resolution history', TRUE),
    ('CARGO_EXCEPTION_MANAGE', 'Record, restrict, escalate, release, reject and resolve cargo exceptions', TRUE);

CREATE SEQUENCE cargo_exception_number_sequence START WITH 1 INCREMENT BY 1;
