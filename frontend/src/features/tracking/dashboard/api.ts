import { api } from '../../../api/client';
import type { DashboardQuery, TrackingDashboard } from './types';

export const trackingDashboardApi = {
  query: async (query:DashboardQuery, signal?:AbortSignal) =>
    (await api.post<TrackingDashboard>('/v1/tracking/dashboard/query', query, { signal })).data,
};
