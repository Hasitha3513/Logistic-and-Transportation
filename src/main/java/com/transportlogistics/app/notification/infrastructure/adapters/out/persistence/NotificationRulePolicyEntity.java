package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.NotificationRulePolicy;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notification_rule_policy")
public class NotificationRulePolicyEntity {
    @Id
    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "quiet_hours_enabled", nullable = false)
    private boolean quietHoursEnabled;

    @Column(name = "quiet_start_time")
    private LocalTime quietStartTime;

    @Column(name = "quiet_end_time")
    private LocalTime quietEndTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_rule_quiet_day", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "day_of_week", nullable = false, length = 9)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> quietDays = new HashSet<>();

    @Column(name = "suppression_window_minutes", nullable = false)
    private int suppressionWindowMinutes;

    @Column(name = "escalation_enabled", nullable = false)
    private boolean escalationEnabled;

    @Column(name = "escalation_after_minutes")
    private Integer escalationAfterMinutes;

    @Column(name = "escalation_recipient_type", length = 32)
    private String escalationRecipientType;

    @Column(name = "escalation_recipient_value", length = 128)
    private String escalationRecipientValue;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    protected NotificationRulePolicyEntity() {}

    static NotificationRulePolicyEntity fromDomain(UUID ruleId, NotificationRulePolicy policy) {
        NotificationRulePolicyEntity entity = new NotificationRulePolicyEntity();
        entity.ruleId = ruleId;
        entity.apply(policy);
        entity.createdAt = OffsetDateTime.now();
        entity.updatedAt = entity.createdAt;
        return entity;
    }

    void apply(NotificationRulePolicy policy) {
        quietHoursEnabled = policy.quietHoursEnabled();
        quietStartTime = policy.quietStartTime();
        quietEndTime = policy.quietEndTime();
        quietDays = new HashSet<>(policy.quietDays());
        suppressionWindowMinutes = policy.suppressionWindowMinutes();
        escalationEnabled = policy.escalationEnabled();
        escalationAfterMinutes = policy.escalationDelayMinutes();
        escalationRecipientType = policy.escalationRecipientType() == null ? null : policy.escalationRecipientType().name();
        escalationRecipientValue = policy.escalationRecipientValue();
        updatedAt = OffsetDateTime.now();
    }

    NotificationRulePolicy toDomain() {
        return new NotificationRulePolicy(quietHoursEnabled, quietStartTime, quietEndTime,
            Set.copyOf(quietDays), suppressionWindowMinutes, escalationEnabled, escalationAfterMinutes,
            escalationRecipientType == null ? null : RecipientType.valueOf(escalationRecipientType),
            escalationRecipientValue);
    }
}
