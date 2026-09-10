-- US-49 geofence transition notification catalogue.
-- Runtime durable publication and consumption remain owned by CS05.

INSERT INTO notification_template
    (id, code, name, event_type, channel, subject, body, version, active, created_at, updated_at)
VALUES
    ('79000000-0000-0000-0000-000000000001',
     'VEHICLE_GEOFENCE_TRANSITIONED_V1',
     'Vehicle geofence transition in-app',
     'VEHICLE_GEOFENCE_TRANSITIONED_V1',
     'IN_APP',
     'Vehicle {{vehicleId}} geofence update',
     'Vehicle {{vehicleId}} {{transition}} geofence {{geofenceId}} ({{geofenceType}}) at {{sourceTimestamp}}.',
     1,
     TRUE,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP);

INSERT INTO notification_rule
    (id, tenant_id, name, description, event_type, channel, recipient_type, recipient_value,
     template_code, enabled, severity_threshold, created_at, updated_at)
SELECT md5(tenant.tenant_id::text || ':VEHICLE_GEOFENCE_TRANSITIONED_V1:IN_APP')::uuid,
       tenant.tenant_id,
       'Vehicle geofence transitions IN_APP',
       'US-49 confirmed geofence transition notification',
       'VEHICLE_GEOFENCE_TRANSITIONED_V1',
       'IN_APP',
       'ROLE',
       'DISPATCHER',
       'VEHICLE_GEOFENCE_TRANSITIONED_V1',
       TRUE,
       'INFO',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM tenant;

INSERT INTO notification_rule_policy
    (rule_id, tenant_id, quiet_hours_enabled, quiet_start_time, quiet_end_time,
     suppression_window_minutes, escalation_enabled, escalation_after_minutes,
     escalation_recipient_type, escalation_recipient_value, created_at, updated_at, version)
SELECT rule.id,
       rule.tenant_id,
       FALSE,
       NULL,
       NULL,
       0,
       FALSE,
       NULL,
       NULL,
       NULL,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       0
FROM notification_rule rule
WHERE rule.event_type = 'VEHICLE_GEOFENCE_TRANSITIONED_V1'
  AND rule.channel = 'IN_APP';
