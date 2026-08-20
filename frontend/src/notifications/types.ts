export type NotificationChannel = 'IN_APP' | 'EMAIL';
export type NotificationSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type RecipientType = 'USER' | 'ROLE' | 'EMAIL_ADDRESS';
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED' | 'READ';

export interface NotificationRule {
  id: string;
  name: string;
  description?: string;
  eventType: string;
  channel: NotificationChannel;
  recipientType: RecipientType;
  recipientValue: string;
  enabled: boolean;
  severityThreshold: NotificationSeverity;
  createdAt: string;
  updatedAt: string;
}

export interface CreateNotificationRuleRequest {
  name: string;
  description?: string;
  eventType: string;
  channel: NotificationChannel;
  recipientType: RecipientType;
  recipientValue: string;
  enabled?: boolean;
  severityThreshold?: NotificationSeverity;
}

export interface UpdateNotificationRuleRequest {
  name: string;
  description?: string;
  eventType: string;
  channel: NotificationChannel;
  recipientType: RecipientType;
  recipientValue: string;
  severityThreshold?: NotificationSeverity;
}

export interface NotificationItem {
  id: string;
  ruleId?: string;
  eventId: string;
  eventType: string;
  channel: NotificationChannel;
  recipient: string;
  severity: NotificationSeverity;
  title: string;
  message: string;
  status: NotificationStatus;
  createdAt: string;
  sentAt?: string;
  readAt?: string;
  failureReason?: string;
  relatedRoute?: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}
