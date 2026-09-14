INSERT INTO app_permission (code, description, active)
VALUES
    ('ROUTE_DEVIATION_VIEW', 'View same-Tenant route-deviation rules and current state', TRUE),
    ('ROUTE_DEVIATION_MANAGE', 'Configure and manage same-Tenant route-deviation rules', TRUE),
    ('ROUTE_DEVIATION_EVENT_VIEW', 'View same-Tenant route-deviation episode evidence', TRUE),
    ('ROUTE_DEVIATION_APPROVE', 'Approve, reject, or correct same-Tenant route-deviation reviews', TRUE)
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    active = EXCLUDED.active;

INSERT INTO app_role_permission (role_id, permission_code)
SELECT role.id, permission.code
FROM app_role role
CROSS JOIN app_permission permission
WHERE role.name IN ('ADMIN', 'LOCAL_MVP_ADMIN')
  AND permission.code IN (
      'ROUTE_DEVIATION_VIEW', 'ROUTE_DEVIATION_MANAGE',
      'ROUTE_DEVIATION_EVENT_VIEW', 'ROUTE_DEVIATION_APPROVE')
ON CONFLICT (role_id, permission_code) DO NOTHING;
