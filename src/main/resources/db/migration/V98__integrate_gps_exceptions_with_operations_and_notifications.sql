-- US-55 CS05: existing-Tenant IN_APP notification catalogue and Tracking Operations intake.
-- Future-Tenant automatic default-rule provisioning is deliberately deferred pending a governed
-- Tenant-creation workflow; this migration seeds only tenants present when V98 executes.
ALTER TABLE operational_exception_case DROP CONSTRAINT ck_operational_exception_source_module;
ALTER TABLE operational_exception_case ADD CONSTRAINT ck_operational_exception_source_module
 CHECK (source_module IN ('ROUTING','DELIVERY','FUEL','TRACKING'));

ALTER TABLE operational_exception_case DROP CONSTRAINT ck_operational_exception_category;
ALTER TABLE operational_exception_case ADD CONSTRAINT ck_operational_exception_category
 CHECK (category IN ('OPERATIONAL','SAFETY','COMPLIANCE','CUSTOMER','FINANCIAL','TECHNICAL','SECURITY',
  'TRACKING_CONNECTIVITY','TRACKING_DEVICE_HEALTH','TRACKING_DEVICE_SECURITY','TRACKING_DATA_QUALITY'));

INSERT INTO notification_template
 (id,code,name,event_type,channel,subject,body,version,active,created_at,updated_at)
VALUES ('98000000-0000-0000-0000-000000000001','TRACKING_GPS_EXCEPTION_ALERT_V1',
 'GPS exception alert in-app','TRACKING_GPS_EXCEPTION_OPENED','IN_APP',
 'GPS {{exceptionTypeLabel}} — {{vehicleLabel}}',
 '{{severity}} GPS exception detected for {{vehicleLabel}} at {{observedAt}}. Review Tracking operations for details.',
 1,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
ON CONFLICT (code,channel,version) DO NOTHING;

INSERT INTO notification_rule
 (id,tenant_id,name,description,event_type,channel,recipient_type,recipient_value,
  template_code,enabled,severity_threshold,created_at,updated_at)
SELECT md5(tenant.tenant_id::text||':TRACKING_GPS_EXCEPTION_OPENED:IN_APP')::uuid,
 tenant.tenant_id,'GPS exception alert IN_APP','US-55 first-open GPS exception alert',
 'TRACKING_GPS_EXCEPTION_OPENED','IN_APP','ROLE','DISPATCHER',
 'TRACKING_GPS_EXCEPTION_ALERT_V1',TRUE,'WARNING',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM tenant
WHERE NOT EXISTS (SELECT 1 FROM notification_rule rule
 WHERE rule.tenant_id=tenant.tenant_id AND rule.event_type='TRACKING_GPS_EXCEPTION_OPENED'
 AND rule.channel='IN_APP');

INSERT INTO notification_rule_policy
 (rule_id,quiet_hours_enabled,suppression_window_minutes,escalation_enabled,created_at,updated_at,version)
SELECT rule.id,FALSE,0,FALSE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0
FROM notification_rule rule
WHERE rule.event_type='TRACKING_GPS_EXCEPTION_OPENED' AND rule.channel='IN_APP'
 AND NOT EXISTS (SELECT 1 FROM notification_rule_policy policy WHERE policy.rule_id=rule.id);
