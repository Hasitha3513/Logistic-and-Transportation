export type DeliveryFailureReason =
  | 'CUSTOMER_UNAVAILABLE'
  | 'WRONG_ADDRESS'
  | 'CUSTOMER_REFUSED'
  | 'ACCESS_RESTRICTED'
  | 'DAMAGED_CARGO'
  | 'DOCUMENT_OR_PAYMENT_ISSUE'
  | 'OTHER';

export type DeliveryFailureDisposition =
  | 'REDELIVERY_ELIGIBLE'
  | 'RETURN_TO_BASE_REQUIRED'
  | 'ESCALATED';

export type DeliveryContactChannel =
  | 'PHONE'
  | 'SMS'
  | 'WHATSAPP'
  | 'EMAIL'
  | 'IN_PERSON';

export type DeliveryContactOutcome =
  | 'ANSWERED_UNABLE_TO_ACCEPT'
  | 'NO_ANSWER'
  | 'BUSY'
  | 'WRONG_NUMBER'
  | 'CALL_DROPPED'
  | 'MESSAGE_LEFT';

export type DeliveryEscalationStatus =
  | 'OPEN'
  | 'UNDER_REVIEW'
  | 'RESOLVED';

export interface DeliveryContactAttempt {
  id: string;
  deliveryAttemptId: string;
  channel: DeliveryContactChannel;
  contactTimestamp: string;
  outcome: DeliveryContactOutcome;
  notes?: string | null;
  recordedBy: string;
  recordedAt: string;
}

export interface DeliveryAttempt {
  id: string;
  deliveryId: string;
  attemptNumber: number;
  attemptTimestamp: string;
  failureReason: DeliveryFailureReason;
  notes?: string | null;
  disposition: DeliveryFailureDisposition;
  contactAttempts: DeliveryContactAttempt[];
  recordedBy: string;
  recordedAt: string;
}

export interface DeliveryEscalation {
  id: string;
  deliveryId: string;
  deliveryAttemptId?: string | null;
  reason: string;
  status: DeliveryEscalationStatus;
  resolutionNotes?: string | null;
  escalatedBy: string;
  escalatedAt: string;
  resolvedBy?: string | null;
  resolvedAt?: string | null;
}

export interface DeliveryFailureHistory {
  deliveryId: string;
  totalAttempts: number;
  attempts: DeliveryAttempt[];
  escalations: DeliveryEscalation[];
}

export interface RecordFailedAttemptPayload {
  expectedVersion: number;
  failureReason: DeliveryFailureReason;
  notes?: string | null;
  requestedDisposition?: DeliveryFailureDisposition | null;
  attemptTimestamp?: string | null;
  contactAttempts?: Array<{
    channel: DeliveryContactChannel;
    contactTimestamp?: string | null;
    outcome: DeliveryContactOutcome;
    notes?: string | null;
  }>;
}

export interface RecordContactAttemptPayload {
  channel: DeliveryContactChannel;
  contactTimestamp?: string | null;
  outcome: DeliveryContactOutcome;
  notes?: string | null;
}

export interface EscalateDeliveryPayload {
  expectedVersion: number;
  deliveryAttemptId?: string | null;
  reason: string;
}

export interface UpdateEscalationPayload {
  status: DeliveryEscalationStatus;
  resolutionNotes?: string | null;
  nextDisposition?: DeliveryFailureDisposition | null;
}

export interface ReturnToBasePayload {
  expectedVersion: number;
  reason?: string | null;
}
