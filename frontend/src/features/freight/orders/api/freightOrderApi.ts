import { api } from '../../../../api/client';
import type { FreightOrder, FreightOrderFilters, FreightOrderPage, FreightOrderPayload, OrganizationReference } from '../types/freightOrder';

export const freightOrderApi = {
  search: async (filters: FreightOrderFilters) => (await api.get<FreightOrderPage>('/v1/freight/orders', { params: filters })).data,
  get: async (id: string) => (await api.get<FreightOrder>(`/v1/freight/orders/${id}`)).data,
  create: async (payload: FreightOrderPayload) => (await api.post<FreightOrder>('/v1/freight/orders', payload)).data,
  update: async (id: string, payload: FreightOrderPayload) => (await api.patch<FreightOrder>(`/v1/freight/orders/${id}`, payload)).data,
  customers: async () => (await api.get<OrganizationReference[]>('/customers')).data.filter((item) => item.active),
  locations: async () => (await api.get<OrganizationReference[]>('/locations')).data.filter((item) => item.active),
};
