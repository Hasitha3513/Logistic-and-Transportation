INSERT INTO app_permission (code, description, active)
VALUES ('TRACKING_DASHBOARD_VIEW', 'View the same-Tenant operational Tracking dashboard', TRUE)
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    active = EXCLUDED.active;

INSERT INTO app_role_permission (role_id, permission_code)
SELECT role.id, permission.code
FROM app_role role
CROSS JOIN app_permission permission
WHERE role.name IN ('ADMIN', 'LOCAL_MVP_ADMIN', 'DISPATCHER')
  AND permission.code = 'TRACKING_DASHBOARD_VIEW'
ON CONFLICT (role_id, permission_code) DO NOTHING;
