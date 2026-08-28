import { api } from '../../../../api/client';
import type { FreightReportFilter, FreightShipment, FreightSummary, PageResponse } from '../types';

const BASE_URL = '/reports/freight';

export const freightReportApi = {
  summary: async (filter: FreightReportFilter) =>
    (await api.get<FreightSummary>(`${BASE_URL}/summary`, { params: filter })).data,
  shipments: async (filter: FreightReportFilter, page: number, size: number) =>
    (await api.get<PageResponse<FreightShipment>>(`${BASE_URL}/shipments`, {
      params: { ...filter, page, size, sort: 'createdAt', direction: 'DESC' },
    })).data,
  exportCsv: async (filter: FreightReportFilter) =>
    (await api.get<Blob>(`${BASE_URL}/export`, { params: filter, responseType: 'blob' })).data,
};
