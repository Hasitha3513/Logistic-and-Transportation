-- US-77 durable EMAIL attempts and one-level escalation linkage.

ALTER TABLE notification ADD COLUMN parent_notification_id UUID;
ALTER TABLE notification ADD COLUMN escalation_level INTEGER NOT NULL DEFAULT 0;

ALTER TABLE notification ADD CONSTRAINT fk_notification_parent
    FOREIGN KEY (parent_notification_id) REFERENCES notification (id);
ALTER TABLE notification ADD CONSTRAINT chk_notification_escalation_level
    CHECK (escalation_level BETWEEN 0 AND 1);
ALTER TABLE notification ADD CONSTRAINT chk_notification_escalation_parent
    CHECK ((escalation_level = 0 AND parent_notification_id IS NULL)
        OR (escalation_level = 1 AND parent_notification_id IS NOT NULL));
ALTER TABLE notification ADD CONSTRAINT uq_notification_escalation_recipient
    UNIQUE (parent_notification_id, recipient, escalation_level);

CREATE INDEX idx_notification_parent ON notification (parent_notification_id);

CREATE TABLE notification_delivery_attempt (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    state VARCHAR(32) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_category VARCHAR(32),
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    provider_message_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_notif_attempt_notification FOREIGN KEY (notification_id) REFERENCES notification (id),
    CONSTRAINT uq_notif_attempt_number UNIQUE (notification_id, attempt_number),
    CONSTRAINT chk_notif_attempt_number CHECK (attempt_number BETWEEN 1 AND 3),
    CONSTRAINT chk_notif_attempt_state CHECK (state IN ('PENDING','IN_PROGRESS','SUCCEEDED','FAILED'))
);

CREATE INDEX idx_notif_attempt_history ON notification_delivery_attempt (notification_id, attempt_number);
CREATE INDEX idx_notif_attempt_due ON notification_delivery_attempt (state, due_at);
CREATE INDEX idx_notif_attempt_completed ON notification_delivery_attempt (completed_at DESC);
