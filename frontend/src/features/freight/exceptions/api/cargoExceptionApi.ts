import { api } from '../../../../api/client';
import {
  CargoException,
  CargoExceptionFilter,
  CreateCargoExceptionPayload,
  EscalateExceptionPayload,
  HoldExceptionPayload,
  ReleaseExceptionPayload,
  RejectExceptionPayload,
  ResolveExceptionPayload,
} from '../types';

const BASE_URL = '/v1/freight/exceptions';

export const cargoExceptionApi = {
  list: async (filter: CargoExceptionFilter = {}): Promise<CargoException[]> => {
    const params = new URLSearchParams();
    if (filter.freightOrderId) params.append('freightOrderId', filter.freightOrderId);
    if (filter.manifestId) params.append('manifestId', filter.manifestId);
    if (filter.type) params.append('type', filter.type);
    if (filter.status) params.append('status', filter.status);
    if (filter.page != null) params.append('page', String(filter.page));
    if (filter.size != null) params.append('size', String(filter.size));
    const response = await api.get<CargoException[]>(`${BASE_URL}?${params.toString()}`);
    return response.data;
  },

  getById: async (id: string): Promise<CargoException> => {
    const response = await api.get<CargoException>(`${BASE_URL}/${id}`);
    return response.data;
  },

  record: async (payload: CreateCargoExceptionPayload): Promise<CargoException> => {
    const response = await api.post<CargoException>(BASE_URL, payload);
    return response.data;
  },

  hold: async (id: string, payload: HoldExceptionPayload): Promise<CargoException> => {
    const response = await api.post<CargoException>(`${BASE_URL}/${id}/hold`, payload);
    return response.data;
  },

  escalate: async (id: string, payload: EscalateExceptionPayload): Promise<CargoException> => {
    const response = await api.post<CargoException>(`${BASE_URL}/${id}/escalate`, payload);
    return response.data;
  },

  release: async (id: string, payload: ReleaseExceptionPayload): Promise<CargoException> => {
    const response = await api.post<CargoException>(`${BASE_URL}/${id}/release`, payload);
    return response.data;
  },

  reject: async (id: string, payload: RejectExceptionPayload): Promise<CargoException> => {
    const response = await api.post<CargoException>(`${BASE_URL}/${id}/reject`, payload);
    return response.data;
  },

  resolve: async (id: string, payload: ResolveExceptionPayload): Promise<CargoException> => {
    const response = await api.post<CargoException>(`${BASE_URL}/${id}/resolve`, payload);
    return response.data;
  },
};
