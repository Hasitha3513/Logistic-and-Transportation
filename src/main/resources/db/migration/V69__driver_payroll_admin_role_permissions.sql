-- Ensure existing administrative roles can delegate and exercise the US-46 permissions.
INSERT INTO app_role_permission (role_id, permission_code)
SELECT role.id, permission.code
FROM app_role role
CROSS JOIN (VALUES
    ('DRIVER_PAYROLL_VIEW'),
    ('DRIVER_PAYROLL_PREPARE'),
    ('DRIVER_PAYROLL_APPROVE'),
    ('DRIVER_PAYROLL_EXPORT')
) AS permission(code)
WHERE role.name IN ('ADMIN', 'LOCAL_MVP_ADMIN')
ON CONFLICT (role_id, permission_code) DO NOTHING;
