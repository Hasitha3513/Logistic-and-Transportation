export interface DeliveryNotificationTimelineItem {
  notificationId: string;
  eventType: string;
  channel: 'EMAIL' | 'SMS';
  status: 'PENDING' | 'SENT' | 'FAILED';
  attemptCount: number;
  createdAt: string;
  sentAt?: string;
  recipient?: string;
  failureCategory?: string;
}
