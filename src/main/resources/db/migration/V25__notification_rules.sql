-- V25__notification_rules.sql
-- Notification rules and operational alerts (US-77)

CREATE TABLE notification_rule (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    event_type VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    recipient_type VARCHAR(32) NOT NULL,
    recipient_value VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    severity_threshold VARCHAR(32) NOT NULL DEFAULT 'INFO',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_notif_rule_channel CHECK (channel IN ('IN_APP', 'EMAIL')),
    CONSTRAINT chk_notif_rule_recipient_type CHECK (recipient_type IN ('USER', 'ROLE', 'EMAIL_ADDRESS')),
    CONSTRAINT chk_notif_rule_severity CHECK (severity_threshold IN ('INFO', 'WARNING', 'CRITICAL'))
);

CREATE INDEX idx_notif_rule_event_enabled ON notification_rule (event_type, enabled);

CREATE TABLE notification (
    id UUID PRIMARY KEY,
    rule_id UUID,
    event_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    recipient VARCHAR(128) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(500),
    related_route VARCHAR(255),
    CONSTRAINT fk_notification_rule FOREIGN KEY (rule_id) REFERENCES notification_rule (id) ON DELETE SET NULL,
    CONSTRAINT chk_notification_channel CHECK (channel IN ('IN_APP', 'EMAIL')),
    CONSTRAINT chk_notification_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'READ')),
    CONSTRAINT uq_notification_event_rule_recipient UNIQUE (event_id, rule_id, recipient)
);

CREATE INDEX idx_notification_recipient_status ON notification (recipient, status);
CREATE INDEX idx_notification_created_at ON notification (created_at DESC);
CREATE INDEX idx_notification_event_id ON notification (event_id);

-- Seed permissions
INSERT INTO app_permission (code, description, active) VALUES
    ('NOTIFICATION_RULE_VIEW', 'View notification rules and trigger configurations', TRUE),
    ('NOTIFICATION_RULE_MANAGE', 'Create, update, enable, disable, and delete notification rules', TRUE),
    ('NOTIFICATION_VIEW', 'View in-app notifications and mark them as read', TRUE);
