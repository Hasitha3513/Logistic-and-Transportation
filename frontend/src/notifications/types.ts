export type NotificationChannel = 'IN_APP' | 'EMAIL';
export type NotificationSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type RecipientType = 'USER' | 'ROLE' | 'EMAIL_ADDRESS';
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED' | 'READ';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
export type DeliveryAttemptState = 'PENDING' | 'IN_PROGRESS' | 'SUCCEEDED' | 'FAILED';
export type EmailDeliveryErrorCategory = 'TRANSIENT' | 'PERMANENT' | 'CONFIGURATION';

export interface NotificationRule {
  id: string;
  name: string;
  description?: string;
  eventType: string;
  channel: NotificationChannel;
  recipientType: RecipientType;
  recipientValue: string;
  templateCode: string;
  quietHoursEnabled: boolean;
  quietStartTime?: string;
  quietEndTime?: string;
  quietDays: DayOfWeek[];
  suppressionWindowMinutes: number;
  escalationEnabled: boolean;
  escalationDelayMinutes?: number;
  escalationRecipientType?: Extract<RecipientType, 'USER' | 'ROLE'>;
  escalationRecipientValue?: string;
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
  templateCode: string;
  quietHoursEnabled?: boolean;
  quietStartTime?: string;
  quietEndTime?: string;
  quietDays?: DayOfWeek[];
  suppressionWindowMinutes?: number;
  escalationEnabled?: boolean;
  escalationDelayMinutes?: number;
  escalationRecipientType?: Extract<RecipientType, 'USER' | 'ROLE'>;
  escalationRecipientValue?: string;
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
  templateCode: string;
  quietHoursEnabled?: boolean;
  quietStartTime?: string;
  quietEndTime?: string;
  quietDays?: DayOfWeek[];
  suppressionWindowMinutes?: number;
  escalationEnabled?: boolean;
  escalationDelayMinutes?: number;
  escalationRecipientType?: Extract<RecipientType, 'USER' | 'ROLE'>;
  escalationRecipientValue?: string;
  enabled?: boolean;
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

export interface NotificationEventCatalogueItem {
  eventType: string;
  owningModule: string;
  defaultSeverity: NotificationSeverity;
  supportedChannels: NotificationChannel[];
  templateCodes: string[];
  requiredVariables: string[];
  optionalVariables: string[];
}

export interface NotificationTemplate {
  id: string;
  code: string;
  name: string;
  eventType: string;
  channel: NotificationChannel;
  subject?: string;
  body: string;
  version: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationDelivery {
  notificationId: string;
  ruleId?: string;
  eventId: string;
  eventType: string;
  channel: NotificationChannel;
  status: NotificationStatus;
  attemptCount: number;
  nextDeliveryAt?: string;
  terminalFailure: boolean;
  parentNotificationId?: string;
  escalationLevel: number;
  createdAt: string;
  sentAt?: string;
  recipient?: string;
}

export interface NotificationDeliveryAttempt {
  id: string;
  attemptNumber: number;
  state: DeliveryAttemptState;
  dueAt: string;
  startedAt?: string;
  completedAt?: string;
  errorCategory?: EmailDeliveryErrorCategory;
  errorCode?: string;
  errorMessage?: string;
  providerMessageId?: string;
}

export interface DeliveryFilters {
  status?: NotificationStatus;
  eventType?: string;
  from?: string;
  to?: string;
  limit?: number;
}
