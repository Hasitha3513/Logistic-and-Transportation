INSERT INTO app_permission (code, description, active)
VALUES
    ('SPEED_MONITOR_VIEW', 'View same-Tenant speed rules and current monitoring states', TRUE),
    ('SPEED_MONITOR_MANAGE', 'Create, update, activate, disable, and retire same-Tenant speed rules', TRUE),
    ('SPEED_EVENT_VIEW', 'View same-Tenant speeding episode evidence', TRUE)
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    active = EXCLUDED.active;

INSERT INTO app_role_permission (role_id, permission_code)
SELECT role.id, permission.code
FROM app_role role
CROSS JOIN app_permission permission
WHERE role.name IN ('ADMIN', 'LOCAL_MVP_ADMIN')
  AND permission.code IN ('SPEED_MONITOR_VIEW', 'SPEED_MONITOR_MANAGE', 'SPEED_EVENT_VIEW')
ON CONFLICT (role_id, permission_code) DO NOTHING;
