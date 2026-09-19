INSERT INTO app_permission(code, description, active)
SELECT code, description, TRUE FROM (VALUES
 ('IDLE_MONITOR_VIEW', 'View same-Tenant idle monitoring state'),
 ('IDLE_EVENT_VIEW', 'View same-Tenant idle episode and minimized evidence history')
) AS proposed(code, description)
WHERE NOT EXISTS (SELECT 1 FROM app_permission existing WHERE existing.code = proposed.code);

INSERT INTO app_role_permission(role_id, permission_code)
SELECT role.id, permission.code
FROM app_role role CROSS JOIN app_permission permission
WHERE role.name IN ('ADMIN', 'DISPATCHER')
  AND permission.code IN ('IDLE_MONITOR_VIEW', 'IDLE_EVENT_VIEW')
  AND permission.active = TRUE
ON CONFLICT(role_id, permission_code) DO NOTHING;
