import { api } from '../../../../api/client';
import type { DeliveryOrder, DeliveryOrderFilters, DeliveryOrderPage, DeliveryOrderPayload, OrganizationReference } from '../types/deliveryOrder';

export const deliveryOrderApi = {
  search: async (filters: DeliveryOrderFilters) => (await api.get<DeliveryOrderPage>('/v1/deliveries', { params: filters })).data,
  get: async (id: string) => (await api.get<DeliveryOrder>(`/v1/deliveries/${id}`)).data,
  create: async (payload: DeliveryOrderPayload) => (await api.post<DeliveryOrder>('/v1/deliveries', payload)).data,
  update: async (id: string, payload: DeliveryOrderPayload) => (await api.patch<DeliveryOrder>(`/v1/deliveries/${id}`, payload)).data,
  validateReadiness: async (id: string, version: number) => (await api.post<DeliveryOrder>(`/v1/deliveries/${id}/validate-readiness`, { version })).data,
  customers: async () => (await api.get<OrganizationReference[]>('/customers')).data.filter((item) => item.active),
  locations: async () => (await api.get<OrganizationReference[]>('/locations')).data.filter((item) => item.active),
};
