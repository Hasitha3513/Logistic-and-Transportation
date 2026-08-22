-- US-77 MVP notification policy evaluation, suppression, quiet hours, and execution audit.

CREATE TABLE notification_rule_policy (
    rule_id UUID PRIMARY KEY,
    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_start_time TIME,
    quiet_end_time TIME,
    suppression_window_minutes INTEGER NOT NULL DEFAULT 0,
    escalation_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    escalation_after_minutes INTEGER,
    escalation_recipient_type VARCHAR(32),
    escalation_recipient_value VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_notif_policy_rule FOREIGN KEY (rule_id) REFERENCES notification_rule (id) ON DELETE CASCADE,
    CONSTRAINT chk_notif_suppression_window CHECK (suppression_window_minutes BETWEEN 0 AND 1440),
    CONSTRAINT chk_notif_quiet_times CHECK (
        (quiet_hours_enabled = FALSE AND quiet_start_time IS NULL AND quiet_end_time IS NULL)
        OR (quiet_hours_enabled = TRUE AND quiet_start_time IS NOT NULL AND quiet_end_time IS NOT NULL AND quiet_start_time <> quiet_end_time)
    )
);

CREATE TABLE notification_rule_quiet_day (
    rule_id UUID NOT NULL,
    day_of_week VARCHAR(9) NOT NULL,
    PRIMARY KEY (rule_id, day_of_week),
    CONSTRAINT fk_notif_quiet_day_policy FOREIGN KEY (rule_id) REFERENCES notification_rule_policy (rule_id) ON DELETE CASCADE,
    CONSTRAINT chk_notif_quiet_day CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))
);

INSERT INTO notification_rule_policy (
    rule_id, quiet_hours_enabled, suppression_window_minutes, escalation_enabled, created_at, updated_at, version
)
SELECT id, FALSE,
    CASE event_type
        WHEN 'TRIP_DELAY_RECORDED' THEN 15
        WHEN 'VEHICLE_MAINTENANCE_DUE' THEN 1440
        WHEN 'VEHICLE_DOCUMENT_EXPIRING' THEN 1440
        WHEN 'DRIVER_MEDICAL_EXPIRING' THEN 1440
        WHEN 'DRIVER_LICENSE_EXPIRING' THEN 1440
        ELSE 0
    END,
    FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM notification_rule;

ALTER TABLE notification ADD COLUMN next_delivery_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_notification_next_delivery ON notification (status, next_delivery_at);

CREATE TABLE notification_rule_execution (
    id UUID PRIMARY KEY,
    execution_key VARCHAR(64) NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    rule_id UUID NOT NULL,
    resolved_recipient VARCHAR(320),
    channel VARCHAR(32) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    suppression_key VARCHAR(64),
    controlling_notification_id UUID,
    failure_code VARCHAR(64),
    failure_message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_notif_execution_key UNIQUE (execution_key),
    CONSTRAINT fk_notif_execution_rule FOREIGN KEY (rule_id) REFERENCES notification_rule (id),
    CONSTRAINT fk_notif_execution_control FOREIGN KEY (controlling_notification_id) REFERENCES notification (id) ON DELETE SET NULL,
    CONSTRAINT chk_notif_execution_channel CHECK (channel IN ('IN_APP', 'EMAIL')),
    CONSTRAINT chk_notif_execution_outcome CHECK (outcome IN ('ACCEPTED','SUPPRESSED','NO_RECIPIENT','TEMPLATE_DATA_MISSING','FAILED'))
);

CREATE INDEX idx_notif_execution_rule_created ON notification_rule_execution (rule_id, created_at DESC);
CREATE INDEX idx_notif_execution_event ON notification_rule_execution (event_id);
CREATE INDEX idx_notif_execution_suppression ON notification_rule_execution (suppression_key, outcome, completed_at DESC);
