-- US-52 route-deviation Notification catalogue. IN_APP Dispatcher delivery only.
INSERT INTO notification_template
    (id, code, name, event_type, channel, subject, body, version, active, created_at, updated_at)
VALUES
    ('90000000-0000-0000-0000-000000000001', 'TRACKING_ROUTE_DEVIATION_DETECTED_V1',
     'Route deviation detected in-app', 'VEHICLE_ROUTE_DEVIATION_DETECTED_V1', 'IN_APP',
     'Route deviation detected — {{domainSeverity}}',
     'Vehicle {{vehicleId}} deviated from route {{routeId}} ({{routeVersion}}) by {{observedDistanceMeters}} m at {{sourceTimestamp}}. Review episode {{routeDeviationEpisodeId}}.{{highSuffix}}',
     1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('90000000-0000-0000-0000-000000000002', 'TRACKING_ROUTE_DEVIATION_ESCALATED_V1',
     'Route deviation escalated in-app', 'VEHICLE_ROUTE_DEVIATION_ESCALATED_V1', 'IN_APP',
     'Route deviation escalated — {{escalationReason}}',
     'Route deviation episode {{routeDeviationEpisodeId}} for vehicle {{vehicleId}} requires immediate attention. Route {{routeId}} ({{routeVersion}}); observed distance {{observedDistanceMeters}} m at {{sourceTimestamp}}. Reason: {{escalationReason}}.',
     1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, channel, version) DO NOTHING;

INSERT INTO notification_rule
    (id, tenant_id, name, description, event_type, channel, recipient_type, recipient_value,
     template_code, enabled, severity_threshold, created_at, updated_at)
SELECT md5(tenant.tenant_id::text || ':VEHICLE_ROUTE_DEVIATION_DETECTED_V1:IN_APP')::uuid,
       tenant.tenant_id, 'Route deviation detection IN_APP', 'US-52 confirmed route deviation',
       'VEHICLE_ROUTE_DEVIATION_DETECTED_V1', 'IN_APP', 'ROLE', 'DISPATCHER',
       'TRACKING_ROUTE_DEVIATION_DETECTED_V1', TRUE, 'WARNING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenant
WHERE NOT EXISTS (SELECT 1 FROM notification_rule rule WHERE rule.tenant_id = tenant.tenant_id
  AND rule.event_type = 'VEHICLE_ROUTE_DEVIATION_DETECTED_V1' AND rule.channel = 'IN_APP');

INSERT INTO notification_rule
    (id, tenant_id, name, description, event_type, channel, recipient_type, recipient_value,
     template_code, enabled, severity_threshold, created_at, updated_at)
SELECT md5(tenant.tenant_id::text || ':VEHICLE_ROUTE_DEVIATION_ESCALATED_V1:IN_APP')::uuid,
       tenant.tenant_id, 'Route deviation escalation IN_APP', 'US-52 route deviation escalation',
       'VEHICLE_ROUTE_DEVIATION_ESCALATED_V1', 'IN_APP', 'ROLE', 'DISPATCHER',
       'TRACKING_ROUTE_DEVIATION_ESCALATED_V1', TRUE, 'CRITICAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenant
WHERE NOT EXISTS (SELECT 1 FROM notification_rule rule WHERE rule.tenant_id = tenant.tenant_id
  AND rule.event_type = 'VEHICLE_ROUTE_DEVIATION_ESCALATED_V1' AND rule.channel = 'IN_APP');

INSERT INTO notification_rule_policy
    (rule_id, quiet_hours_enabled, suppression_window_minutes, escalation_enabled,
     created_at, updated_at, version)
SELECT rule.id, FALSE, 0, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM notification_rule rule
WHERE rule.event_type IN ('VEHICLE_ROUTE_DEVIATION_DETECTED_V1',
                           'VEHICLE_ROUTE_DEVIATION_ESCALATED_V1')
  AND rule.channel = 'IN_APP'
  AND NOT EXISTS (SELECT 1 FROM notification_rule_policy policy WHERE policy.rule_id = rule.id);
