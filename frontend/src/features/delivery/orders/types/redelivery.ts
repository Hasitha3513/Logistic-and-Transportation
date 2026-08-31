export type RedeliverySchedulingMethod = 'AUTOMATIC' | 'AGENT_ASSISTED';
export type RedeliveryScheduleStatus = 'CONFIRMED' | 'SUPERSEDED' | 'CANCELLED';

export interface RedeliverySchedule {
  id: string;
  deliveryOrderId: string;
  deliveryAttemptId: string;
  schedulingMethod: RedeliverySchedulingMethod;
  preferredStartTime: string | null;
  preferredEndTime: string | null;
  customerPreferenceNotes: string | null;
  scheduledStartTime: string;
  scheduledEndTime: string;
  status: RedeliveryScheduleStatus;
  scheduledBy: string;
  scheduledAt: string;
  supersededAt: string | null;
  supersededBy: string | null;
  supersedeReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RedeliverySuggestion {
  startTime: string;
  endTime: string;
  slotLabel: string;
  available: boolean;
  note: string;
}

export interface ScheduleRedeliveryPayload {
  expectedVersion: number;
  failedAttemptId?: string;
  schedulingMethod: RedeliverySchedulingMethod;
  preferredStartTime?: string | null;
  preferredEndTime?: string | null;
  customerPreferenceNotes?: string | null;
  scheduledStartTime: string;
  scheduledEndTime: string;
}

export interface RescheduleRedeliveryPayload {
  expectedVersion: number;
  supersedeReason?: string | null;
  scheduledStartTime: string;
  scheduledEndTime: string;
}

export interface RedeliverySuggestionPayload {
  preferredStartTime?: string | null;
  preferredEndTime?: string | null;
  customerPreferenceNotes?: string | null;
}
