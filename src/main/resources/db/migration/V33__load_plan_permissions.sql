-- V33__load_plan_permissions.sql
-- Add permissions for load plan management

INSERT INTO app_permission (code, description, active) VALUES
    ('LOAD_PLAN_VIEW', 'View load plans', TRUE),
    ('LOAD_PLAN_MANAGE', 'Create, update and manage load plans', TRUE);
