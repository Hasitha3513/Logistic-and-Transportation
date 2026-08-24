-- Controlled MVP notification templates and rule/template linkage (MVP-GAP-008B).
-- V25 allowed free-form event types. Supported rows are backfilled deterministically;
-- unsupported legacy rows are preserved but disabled and must be corrected through the API.

CREATE TABLE notification_template (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    version INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_notif_template_channel CHECK (channel IN ('IN_APP', 'EMAIL')),
    CONSTRAINT chk_notif_template_version CHECK (version > 0),
    CONSTRAINT chk_notif_template_code_length CHECK (CHAR_LENGTH(code) BETWEEN 1 AND 64),
    CONSTRAINT chk_notif_template_name_length CHECK (CHAR_LENGTH(name) BETWEEN 1 AND 128),
    CONSTRAINT chk_notif_template_subject_length CHECK (CHAR_LENGTH(subject) BETWEEN 1 AND 255),
    CONSTRAINT chk_notif_template_body_length CHECK (CHAR_LENGTH(body) BETWEEN 1 AND 4000),
    CONSTRAINT uq_notif_template_version UNIQUE (code, channel, version)
);

CREATE INDEX idx_notif_template_event_channel_active
    ON notification_template (event_type, channel, active);

INSERT INTO notification_template
    (id, code, name, event_type, channel, subject, body, version, active, created_at, updated_at)
VALUES
    ('76000000-0000-0000-0000-000000000001', 'TRIP_DELAY', 'Trip delay in-app', 'TRIP_DELAY_RECORDED', 'IN_APP',
     'Trip {{tripNumber}} delayed', 'Trip {{tripNumber}} is delayed by {{delayMinutes}} minutes. Reason: {{reason}}. {{locationDescription}}', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000002', 'TRIP_DELAY', 'Trip delay email', 'TRIP_DELAY_RECORDED', 'EMAIL',
     'Trip {{tripNumber}} delayed', 'Trip {{tripNumber}} is delayed by {{delayMinutes}} minutes. Reason: {{reason}}. {{locationDescription}} Event time: {{eventTime}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000003', 'TRIP_INCIDENT', 'Trip incident in-app', 'TRIP_INCIDENT_RECORDED', 'IN_APP',
     '{{incidentSeverity}} incident on trip {{tripNumber}}', '{{description}} {{locationDescription}}', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000004', 'TRIP_INCIDENT', 'Trip incident email', 'TRIP_INCIDENT_RECORDED', 'EMAIL',
     '{{incidentSeverity}} incident on trip {{tripNumber}}', 'Trip {{tripNumber}} incident: {{description}} {{locationDescription}} Event time: {{eventTime}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000005', 'VEHICLE_MAINTENANCE_DUE', 'Maintenance due in-app', 'VEHICLE_MAINTENANCE_DUE', 'IN_APP',
     'Maintenance due for {{vehicleRegistration}}', '{{maintenanceType}} is scheduled from {{scheduledStart}} to {{scheduledEnd}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000006', 'VEHICLE_MAINTENANCE_DUE', 'Maintenance due email', 'VEHICLE_MAINTENANCE_DUE', 'EMAIL',
     'Maintenance due for {{vehicleRegistration}}', '{{maintenanceType}} is scheduled for vehicle {{vehicleRegistration}} from {{scheduledStart}} to {{scheduledEnd}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000007', 'VEHICLE_DOCUMENT_EXPIRING', 'Vehicle document expiry in-app', 'VEHICLE_DOCUMENT_EXPIRING', 'IN_APP',
     '{{documentType}} expiring for {{vehicleRegistration}}', 'Document {{documentNumber}} expires on {{expiryDate}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000008', 'VEHICLE_DOCUMENT_EXPIRING', 'Vehicle document expiry email', 'VEHICLE_DOCUMENT_EXPIRING', 'EMAIL',
     '{{documentType}} expiring for {{vehicleRegistration}}', 'Vehicle {{vehicleRegistration}} document {{documentNumber}} expires on {{expiryDate}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000009', 'DRIVER_EXCEPTION', 'Driver exception in-app', 'DRIVER_EXCEPTION_RECORDED', 'IN_APP',
     '{{exceptionType}} recorded for {{driverName}}', 'Driver exception runs from {{startTime}} to {{endTime}}. {{reason}}', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000010', 'DRIVER_EXCEPTION', 'Driver exception email', 'DRIVER_EXCEPTION_RECORDED', 'EMAIL',
     '{{exceptionType}} recorded for {{driverName}}', 'A {{exceptionType}} exception for {{driverName}} runs from {{startTime}} to {{endTime}}. {{reason}}', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000011', 'DRIVER_MEDICAL_EXPIRING', 'Driver medical expiry in-app', 'DRIVER_MEDICAL_EXPIRING', 'IN_APP',
     'Medical fitness expiring for {{driverName}}', '{{fitnessStatus}} medical fitness is valid until {{validUntil}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000012', 'DRIVER_MEDICAL_EXPIRING', 'Driver medical expiry email', 'DRIVER_MEDICAL_EXPIRING', 'EMAIL',
     'Medical fitness expiring for {{driverName}}', '{{driverName}} has {{fitnessStatus}} medical fitness valid until {{validUntil}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000013', 'DRIVER_DRUG_TEST_FAILED', 'Failed drug test in-app', 'DRIVER_DRUG_TEST_FAILED', 'IN_APP',
     'Drug test failed for {{driverName}}', '{{testType}} test dated {{resultDate}} has a blocking result.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000014', 'DRIVER_DRUG_TEST_FAILED', 'Failed drug test email', 'DRIVER_DRUG_TEST_FAILED', 'EMAIL',
     'Drug test failed for {{driverName}}', '{{driverName}} has a blocking {{testType}} drug-test result dated {{resultDate}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000015', 'DRIVER_LICENSE_EXPIRING', 'Driver licence expiry in-app', 'DRIVER_LICENSE_EXPIRING', 'IN_APP',
     'Licence expiring for {{driverName}}', 'Licence {{licenseNumber}} (class {{licenseClass}}) expires on {{expiryDate}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('76000000-0000-0000-0000-000000000016', 'DRIVER_LICENSE_EXPIRING', 'Driver licence expiry email', 'DRIVER_LICENSE_EXPIRING', 'EMAIL',
     'Licence expiring for {{driverName}}', '{{driverName}} licence {{licenseNumber}} (class {{licenseClass}}) expires on {{expiryDate}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE notification_rule ADD COLUMN template_code VARCHAR(64);

UPDATE notification_rule
SET template_code = CASE event_type
    WHEN 'TRIP_DELAY_RECORDED' THEN 'TRIP_DELAY'
    WHEN 'TRIP_INCIDENT_RECORDED' THEN 'TRIP_INCIDENT'
    WHEN 'VEHICLE_MAINTENANCE_DUE' THEN 'VEHICLE_MAINTENANCE_DUE'
    WHEN 'VEHICLE_DOCUMENT_EXPIRING' THEN 'VEHICLE_DOCUMENT_EXPIRING'
    WHEN 'DRIVER_EXCEPTION_RECORDED' THEN 'DRIVER_EXCEPTION'
    WHEN 'DRIVER_MEDICAL_EXPIRING' THEN 'DRIVER_MEDICAL_EXPIRING'
    WHEN 'DRIVER_DRUG_TEST_FAILED' THEN 'DRIVER_DRUG_TEST_FAILED'
    WHEN 'DRIVER_LICENSE_EXPIRING' THEN 'DRIVER_LICENSE_EXPIRING'
    ELSE NULL
END;

UPDATE notification_rule SET enabled = FALSE WHERE template_code IS NULL;
CREATE INDEX idx_notif_rule_template_code ON notification_rule (template_code);

ALTER TABLE notification ADD COLUMN template_id UUID;
ALTER TABLE notification ADD COLUMN template_version INTEGER;
ALTER TABLE notification ADD CONSTRAINT fk_notification_template
    FOREIGN KEY (template_id) REFERENCES notification_template (id) ON DELETE SET NULL;
ALTER TABLE notification ADD CONSTRAINT chk_notification_template_version
    CHECK (template_version IS NULL OR template_version > 0);
CREATE INDEX idx_notification_template_id ON notification (template_id);
