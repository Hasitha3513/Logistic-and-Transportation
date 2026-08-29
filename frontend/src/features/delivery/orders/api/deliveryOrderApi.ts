import { api } from '../../../../api/client';
import type { DeliveryOrder, DeliveryOrderFilters, DeliveryOrderPage, DeliveryOrderPayload, OrganizationReference, ProofOfDelivery, PodEvidenceType } from '../types/deliveryOrder';

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
  customers: async () => (await api.get<OrganizationReference[]>('/customers')).data.filter((item) => item.active),
  locations: async () => (await api.get<OrganizationReference[]>('/locations')).data.filter((item) => item.active),
};
