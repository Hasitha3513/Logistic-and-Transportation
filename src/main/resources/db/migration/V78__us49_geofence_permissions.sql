INSERT INTO app_permission (code, description, active)
VALUES
    ('GEOFENCE_VIEW', 'View same-Tenant geofence definitions and current memberships', TRUE),
    ('GEOFENCE_MANAGE', 'Create, update, activate, disable, and retire same-Tenant geofences', TRUE),
    ('GEOFENCE_EVENT_VIEW', 'View same-Tenant geofence transition history', TRUE)
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    active = EXCLUDED.active;

INSERT INTO app_role_permission (role_id, permission_code)
SELECT role.id, permission.code
FROM app_role role
CROSS JOIN app_permission permission
WHERE role.name IN ('ADMIN', 'LOCAL_MVP_ADMIN')
  AND permission.code IN ('GEOFENCE_VIEW', 'GEOFENCE_MANAGE', 'GEOFENCE_EVENT_VIEW')
ON CONFLICT (role_id, permission_code) DO NOTHING;
