import { api } from '../../../../api/client';
import type { DeliveryOrder, DeliveryOrderFilters, DeliveryOrderPage, DeliveryOrderPayload, OrganizationReference, ProofOfDelivery, PodEvidenceType } from '../types/deliveryOrder';
import type {
  DeliveryAttempt,
  DeliveryContactAttempt,
  DeliveryEscalation,
  DeliveryFailureHistory,
  RecordFailedAttemptPayload,
  RecordContactAttemptPayload,
  EscalateDeliveryPayload,
  UpdateEscalationPayload,
  ReturnToBasePayload
} from '../types/failedDelivery';
import type {
  RedeliverySchedule,
  RedeliverySuggestion,
  ScheduleRedeliveryPayload,
  RescheduleRedeliveryPayload,
  RedeliverySuggestionPayload
} from '../types/redelivery';

export interface LastMilePlannerContext {
  deliveryOrderId: string;
  deliveryStatus: string;
  failedAttemptCount: number;
  activeExceptionCount: number;
  openEscalationCount: number;
  availableActions: string[];
}

export const deliveryOrderApi = {
  search: async (filters: DeliveryOrderFilters) => (await api.get<DeliveryOrderPage>('/v1/deliveries', { params: filters })).data,
  get: async (id: string) => (await api.get<DeliveryOrder>(`/v1/deliveries/${id}`)).data,
  create: async (payload: DeliveryOrderPayload) => (await api.post<DeliveryOrder>('/v1/deliveries', payload)).data,
  update: async (id: string, payload: DeliveryOrderPayload) => (await api.patch<DeliveryOrder>(`/v1/deliveries/${id}`, payload)).data,
  validateReadiness: async (id: string, version: number) => (await api.post<DeliveryOrder>(`/v1/deliveries/${id}/validate-readiness`, { version })).data,
  getProof: async (id: string) => (await api.get<ProofOfDelivery>(`/v1/deliveries/${id}/proof`)).data,
  createProof: async (id: string, payload: object) => (await api.post<ProofOfDelivery>(`/v1/deliveries/${id}/proof`, payload)).data,
  addEvidence: async (id: string, podVersion: number, type: PodEvidenceType, file?: File, barcodeValue?: string, captureSource = 'FILE') => {
    const body = new FormData(); body.append('podVersion', String(podVersion)); body.append('type', type); body.append('captureSource', captureSource);
    if (file) body.append('file', file); if (barcodeValue) body.append('barcodeValue', barcodeValue);
    return (await api.post<ProofOfDelivery>(`/v1/deliveries/${id}/proof/evidence`, body)).data;
  },
  finalizeProof: async (id: string, deliveryVersion: number, podVersion: number) => (await api.post<{ proof: ProofOfDelivery; delivery: DeliveryOrder }>(`/v1/deliveries/${id}/proof/finalize`, { deliveryVersion, podVersion })).data,
  deleteEvidence: async (id: string, evidenceId: string, podVersion: number) => (await api.delete<ProofOfDelivery>(`/v1/deliveries/${id}/proof/evidence/${evidenceId}`, { params: { podVersion } })).data,

  // US-59 Failed Deliveries API
  getAttempts: async (id: string) => (await api.get<DeliveryFailureHistory>(`/v1/deliveries/${id}/attempts`)).data,
  recordFailedAttempt: async (id: string, payload: RecordFailedAttemptPayload) =>
    (await api.post<DeliveryAttempt>(`/v1/deliveries/${id}/failed-attempt`, payload)).data,
  recordContactAttempt: async (id: string, attemptId: string, payload: RecordContactAttemptPayload) =>
    (await api.post<DeliveryContactAttempt>(`/v1/deliveries/${id}/failed-attempts/${attemptId}/contacts`, payload)).data,
  escalateDelivery: async (id: string, payload: EscalateDeliveryPayload) =>
    (await api.post<DeliveryEscalation>(`/v1/deliveries/${id}/escalate`, payload)).data,
  updateEscalation: async (id: string, escalationId: string, payload: UpdateEscalationPayload) =>
    (await api.patch<DeliveryEscalation>(`/v1/deliveries/${id}/escalations/${escalationId}`, payload)).data,
  returnToBase: async (id: string, payload: ReturnToBasePayload) =>
    (await api.post<DeliveryOrder>(`/v1/deliveries/${id}/return-to-base`, payload)).data,

  // US-60 Redelivery Scheduling API
  getRedeliverySuggestions: async (id: string, payload?: RedeliverySuggestionPayload) =>
    (await api.post<RedeliverySuggestion[]>(`/v1/deliveries/${id}/redelivery/suggestions`, payload || {})).data,
  scheduleRedelivery: async (id: string, payload: ScheduleRedeliveryPayload) =>
    (await api.post<RedeliverySchedule>(`/v1/deliveries/${id}/redelivery/schedule`, payload)).data,
  rescheduleRedelivery: async (id: string, payload: RescheduleRedeliveryPayload) =>
    (await api.post<RedeliverySchedule>(`/v1/deliveries/${id}/redelivery/reschedule`, payload)).data,
  getRedeliveryHistory: async (id: string) =>
    (await api.get<RedeliverySchedule[]>(`/v1/deliveries/${id}/redelivery/history`)).data,
  getLastMilePlannerContext: async (id: string) =>
    (await api.get<LastMilePlannerContext>(`/v1/deliveries/${id}/last-mile-planner`)).data,

  customers: async () => (await api.get<OrganizationReference[]>('/customers')).data.filter((item) => item.active),
  locations: async () => (await api.get<OrganizationReference[]>('/locations')).data.filter((item) => item.active),
};
