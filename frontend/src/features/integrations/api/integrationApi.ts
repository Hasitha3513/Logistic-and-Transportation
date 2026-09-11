import { api } from '../../../api/client';
import type { Integration, IntegrationExchange, IntegrationPayload, Page } from '../types/integration';

export const integrationApi = {
  list: async (page = 0, size = 20) =>
    (await api.get<Page<Integration>>('/v1/integrations', { params: { page, size } })).data,
  get: async (id: string) => (await api.get<Integration>(`/v1/integrations/${id}`)).data,
  create: async (payload: IntegrationPayload) =>
    (await api.post<Integration>('/v1/integrations', payload)).data,
  update: async (id: string, payload: IntegrationPayload) =>
    (await api.put<Integration>(`/v1/integrations/${id}`, payload)).data,
  test: async (id: string) =>
    (await api.post<{ integration: Integration; success: boolean; code: string; testedAt: string }>(
      `/v1/integrations/${id}/test`)).data,
  enable: async (id: string, version: number) =>
    (await api.post<Integration>(`/v1/integrations/${id}/enable`, { version })).data,
  disable: async (id: string, version: number) =>
    (await api.post<Integration>(`/v1/integrations/${id}/disable`, { version })).data,
  exchanges: async (id: string, page = 0, size = 20) =>
    (await api.get<Page<IntegrationExchange>>(`/v1/integrations/${id}/exchanges`, { params: { page, size } })).data,
};
