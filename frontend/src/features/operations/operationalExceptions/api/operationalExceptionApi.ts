import { api } from '../../../../api/client';
import type { OperationalExceptionCase, OperationalExceptionDetail, OperationalExceptionHistory, Page } from '../types/operationalException';

export const operationalExceptionApi = {
  list: async (params: Record<string, string | number | undefined> = {}) =>
    (await api.get<Page<OperationalExceptionCase>>('/v1/operational-exceptions', { params })).data,
  get: async (id: string) => (await api.get<OperationalExceptionDetail>(`/v1/operational-exceptions/${id}`)).data,
  history: async (id: string) =>
    (await api.get<Page<OperationalExceptionHistory>>(`/v1/operational-exceptions/${id}/history`)).data,
  command: async (id: string, action: string, payload: unknown) =>
    (await api.post<OperationalExceptionDetail>(`/v1/operational-exceptions/${id}/${action}`, payload)).data,
  actionCommand: async (id: string, actionId: string, action: 'start' | 'complete', version: number) =>
    (await api.post<OperationalExceptionDetail>(
      `/v1/operational-exceptions/${id}/corrective-actions/${actionId}/${action}`,
      { expectedVersion: version },
    )).data,
};
