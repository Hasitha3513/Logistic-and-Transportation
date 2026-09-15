INSERT INTO app_permission (code, description, active)
VALUES
    ('JOURNEY_REPLAY_VIEW', 'View same-Tenant bounded journey movement, stops, and route context', TRUE),
    ('JOURNEY_REPLAY_INCIDENT_VIEW', 'View same-Tenant eligible journey replay incident overlays', TRUE)
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    active = EXCLUDED.active;

INSERT INTO app_role_permission (role_id, permission_code)
SELECT role.id, permission.code
FROM app_role role
CROSS JOIN app_permission permission
WHERE role.name IN ('ADMIN', 'LOCAL_MVP_ADMIN', 'DISPATCHER')
  AND permission.code IN ('JOURNEY_REPLAY_VIEW', 'JOURNEY_REPLAY_INCIDENT_VIEW')
ON CONFLICT (role_id, permission_code) DO NOTHING;
