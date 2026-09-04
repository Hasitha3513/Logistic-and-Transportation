INSERT INTO app_permission (code, description, active)
VALUES ('FUEL_PERFORMANCE_VIEW', 'View tenant-scoped vehicle and driver fuel performance analytics', TRUE)
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    active = EXCLUDED.active;
