INSERT INTO app_permission(code,description,active)
SELECT code,description,TRUE FROM (VALUES
 ('GPS_EXCEPTION_VIEW','View same-Tenant bounded GPS exception episodes and immutable evidence'),
 ('GPS_EXCEPTION_REVIEW','Acknowledge same-Tenant GPS exception episodes')
) AS proposed(code,description)
WHERE NOT EXISTS (SELECT 1 FROM app_permission existing WHERE existing.code=proposed.code);

INSERT INTO app_role_permission(role_id,permission_code)
SELECT role.id,permission.code
FROM app_role role CROSS JOIN app_permission permission
WHERE role.name IN ('ADMIN','LOCAL_MVP_ADMIN','DISPATCHER')
  AND permission.code IN ('GPS_EXCEPTION_VIEW','GPS_EXCEPTION_REVIEW')
  AND permission.active=TRUE
ON CONFLICT(role_id,permission_code) DO NOTHING;
