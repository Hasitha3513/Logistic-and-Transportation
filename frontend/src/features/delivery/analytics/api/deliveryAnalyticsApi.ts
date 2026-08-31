import { api } from '../../../../api/client';
import {
  DeliveryAnalyticsFilters,
  DeliveryAnalyticsSummary,
  FailureReasonBreakdownItem,
  RegionalPerformanceItem,
  DeliveryTrendItem,
} from '../types/deliveryAnalytics';

export const deliveryAnalyticsApi = {
  getSummary: async (filters: DeliveryAnalyticsFilters = {}): Promise<DeliveryAnalyticsSummary> => {
    const { data } = await api.get<DeliveryAnalyticsSummary>('/v1/deliveries/analytics/summary', {
      params: filters,
    });
    return data;
  },

  getFailures: async (filters: DeliveryAnalyticsFilters = {}): Promise<FailureReasonBreakdownItem[]> => {
    const { data } = await api.get<FailureReasonBreakdownItem[]>('/v1/deliveries/analytics/failures', {
      params: filters,
    });
    return data;
  },

  getRegions: async (filters: DeliveryAnalyticsFilters = {}): Promise<RegionalPerformanceItem[]> => {
    const { data } = await api.get<RegionalPerformanceItem[]>('/v1/deliveries/analytics/regions', {
      params: filters,
    });
    return data;
  },

  getTrends: async (
    filters: DeliveryAnalyticsFilters = {},
    granularity: 'DAY' | 'WEEK' | 'MONTH' = 'DAY'
  ): Promise<DeliveryTrendItem[]> => {
    const { data } = await api.get<DeliveryTrendItem[]>('/v1/deliveries/analytics/trends', {
      params: { ...filters, granularity },
    });
    return data;
  },
};
