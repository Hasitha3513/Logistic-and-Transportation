-- US-69 Delivery customer notifications using the existing US-77 engine.

CREATE TABLE customer_notification_preference (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    email_enabled BOOLEAN NOT NULL,
    sms_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_customer_notif_pref_tenant_customer UNIQUE (tenant_id, customer_id)
);

CREATE INDEX idx_customer_notif_pref_tenant_customer
    ON customer_notification_preference (tenant_id, customer_id);

ALTER TABLE notification_rule DROP CONSTRAINT chk_notif_rule_channel;
ALTER TABLE notification_rule ADD CONSTRAINT chk_notif_rule_channel
    CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS'));
ALTER TABLE notification_rule DROP CONSTRAINT chk_notif_rule_recipient_type;
ALTER TABLE notification_rule ADD CONSTRAINT chk_notif_rule_recipient_type
    CHECK (recipient_type IN ('USER', 'ROLE', 'EMAIL_ADDRESS', 'EVENT_CUSTOMER'));

ALTER TABLE notification_template DROP CONSTRAINT chk_notif_template_channel;
ALTER TABLE notification_template ADD CONSTRAINT chk_notif_template_channel
    CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS'));

ALTER TABLE notification DROP CONSTRAINT chk_notification_channel;
ALTER TABLE notification ADD CONSTRAINT chk_notification_channel
    CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS'));
ALTER TABLE notification ALTER COLUMN recipient TYPE VARCHAR(320);

ALTER TABLE notification_rule_execution DROP CONSTRAINT chk_notif_execution_channel;
ALTER TABLE notification_rule_execution ADD CONSTRAINT chk_notif_execution_channel
    CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS'));

CREATE INDEX idx_notif_execution_tenant_aggregate_created
    ON notification_rule_execution (tenant_id, aggregate_type, aggregate_id, created_at DESC);

INSERT INTO notification_template
    (id, code, name, event_type, channel, subject, body, version, active, created_at, updated_at)
VALUES
    ('58000000-0000-0000-0000-000000000001', 'DELIVERY_OUT_FOR_DELIVERY', 'Delivery out for delivery email',
     'DELIVERY_OUT_FOR_DELIVERY', 'EMAIL', 'Delivery {{deliveryNumber}} is out for delivery',
     'Hello {{customerDisplayName}}, delivery {{deliveryNumber}} is now {{status}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('58000000-0000-0000-0000-000000000002', 'DELIVERY_OUT_FOR_DELIVERY', 'Delivery out for delivery SMS',
     'DELIVERY_OUT_FOR_DELIVERY', 'SMS', 'Delivery {{deliveryNumber}} update',
     '{{customerDisplayName}}, delivery {{deliveryNumber}} is now {{status}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('58000000-0000-0000-0000-000000000003', 'DELIVERY_ETA_RISK_CHANGED', 'Delivery ETA risk email',
     'DELIVERY_ETA_RISK_CHANGED', 'EMAIL', 'ETA update for delivery {{deliveryNumber}}',
     'Hello {{customerDisplayName}}, delivery {{deliveryNumber}} is {{slaStatus}}. Estimated arrival: {{estimatedArrivalAt}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('58000000-0000-0000-0000-000000000004', 'DELIVERY_ETA_RISK_CHANGED', 'Delivery ETA risk SMS',
     'DELIVERY_ETA_RISK_CHANGED', 'SMS', 'ETA update for {{deliveryNumber}}',
     '{{customerDisplayName}}, {{deliveryNumber}} is {{slaStatus}}. ETA: {{estimatedArrivalAt}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('58000000-0000-0000-0000-000000000005', 'DELIVERY_COMPLETED', 'Delivery completed email',
     'DELIVERY_COMPLETED', 'EMAIL', 'Delivery {{deliveryNumber}} completed',
     'Hello {{customerDisplayName}}, delivery {{deliveryNumber}} was {{status}} at {{completedAt}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('58000000-0000-0000-0000-000000000006', 'DELIVERY_COMPLETED', 'Delivery completed SMS',
     'DELIVERY_COMPLETED', 'SMS', 'Delivery {{deliveryNumber}} completed',
     '{{customerDisplayName}}, delivery {{deliveryNumber}} was {{status}} at {{completedAt}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('58000000-0000-0000-0000-000000000007', 'DELIVERY_FAILED_ATTEMPT_RECORDED', 'Delivery failed attempt email',
     'DELIVERY_FAILED_ATTEMPT_RECORDED', 'EMAIL', 'Delivery attempt update for {{deliveryNumber}}',
     'Hello {{customerDisplayName}}, delivery {{deliveryNumber}} is {{status}}. Next disposition: {{failureDisposition}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('58000000-0000-0000-0000-000000000008', 'DELIVERY_FAILED_ATTEMPT_RECORDED', 'Delivery failed attempt SMS',
     'DELIVERY_FAILED_ATTEMPT_RECORDED', 'SMS', 'Delivery attempt update',
     '{{customerDisplayName}}, {{deliveryNumber}} is {{status}}. Next: {{failureDisposition}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('58000000-0000-0000-0000-000000000009', 'DELIVERY_REDELIVERY_SCHEDULED', 'Delivery redelivery email',
     'DELIVERY_REDELIVERY_SCHEDULED', 'EMAIL', 'Redelivery scheduled for {{deliveryNumber}}',
     'Hello {{customerDisplayName}}, redelivery {{deliveryNumber}} is {{status}} from {{scheduledWindowStart}} to {{scheduledWindowEnd}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('58000000-0000-0000-0000-000000000010', 'DELIVERY_REDELIVERY_SCHEDULED', 'Delivery redelivery SMS',
     'DELIVERY_REDELIVERY_SCHEDULED', 'SMS', 'Redelivery {{deliveryNumber}}',
     '{{customerDisplayName}}, redelivery {{deliveryNumber}} is {{status}} from {{scheduledWindowStart}} to {{scheduledWindowEnd}}.', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO notification_rule
    (id, tenant_id, name, description, event_type, channel, recipient_type, recipient_value,
     template_code, enabled, severity_threshold, created_at, updated_at)
SELECT md5(t.tenant_id::text || ':' || event.event_type || ':' || channel.channel)::uuid,
       t.tenant_id,
       event.display_name || ' ' || channel.channel,
       'US-69 customer operational delivery notification',
       event.event_type,
       channel.channel,
       'EVENT_CUSTOMER',
       'customerId',
       event.event_type,
       TRUE,
       event.severity,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM tenant t
CROSS JOIN (VALUES
    ('DELIVERY_OUT_FOR_DELIVERY', 'Delivery out for delivery', 'INFO'),
    ('DELIVERY_ETA_RISK_CHANGED', 'Delivery ETA risk changed', 'WARNING'),
    ('DELIVERY_COMPLETED', 'Delivery completed', 'INFO'),
    ('DELIVERY_FAILED_ATTEMPT_RECORDED', 'Delivery failed attempt', 'WARNING'),
    ('DELIVERY_REDELIVERY_SCHEDULED', 'Delivery redelivery scheduled', 'INFO')
) AS event(event_type, display_name, severity)
CROSS JOIN (VALUES ('EMAIL'), ('SMS')) AS channel(channel);

INSERT INTO notification_rule_policy
    (rule_id, tenant_id, quiet_hours_enabled, quiet_start_time, quiet_end_time,
     suppression_window_minutes, escalation_enabled, escalation_after_minutes,
     escalation_recipient_type, escalation_recipient_value, created_at, updated_at, version)
SELECT r.id,
       r.tenant_id,
       FALSE,
       NULL,
       NULL,
       CASE WHEN r.event_type IN ('DELIVERY_ETA_RISK_CHANGED', 'DELIVERY_REDELIVERY_SCHEDULED') THEN 1440 ELSE 0 END,
       FALSE,
       NULL,
       NULL,
       NULL,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       0
FROM notification_rule r
WHERE r.recipient_type = 'EVENT_CUSTOMER';
