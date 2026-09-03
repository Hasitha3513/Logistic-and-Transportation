import { useQuery } from '@tanstack/react-query';
import { deliveryAnalyticsApi } from '../api/deliveryAnalyticsApi';
import { DeliveryAnalyticsFilters } from '../types/deliveryAnalytics';

export const DELIVERY_ANALYTICS_QUERY_KEYS = {
  all: ['delivery-analytics'] as const,
  summary: (filters: DeliveryAnalyticsFilters) => [...DELIVERY_ANALYTICS_QUERY_KEYS.all, 'summary', filters] as const,
  failures: (filters: DeliveryAnalyticsFilters) => [...DELIVERY_ANALYTICS_QUERY_KEYS.all, 'failures', filters] as const,
  regions: (filters: DeliveryAnalyticsFilters) => [...DELIVERY_ANALYTICS_QUERY_KEYS.all, 'regions', filters] as const,
  trends: (filters: DeliveryAnalyticsFilters, granularity: string) => [...DELIVERY_ANALYTICS_QUERY_KEYS.all, 'trends', filters, granularity] as const,
};

export const useDeliveryAnalyticsSummary = (filters: DeliveryAnalyticsFilters) => {
  return useQuery({
    queryKey: DELIVERY_ANALYTICS_QUERY_KEYS.summary(filters),
    queryFn: () => deliveryAnalyticsApi.getSummary(filters),
  });
};

export const useDeliveryAnalyticsFailures = (filters: DeliveryAnalyticsFilters) => {
  return useQuery({
    queryKey: DELIVERY_ANALYTICS_QUERY_KEYS.failures(filters),
    queryFn: () => deliveryAnalyticsApi.getFailures(filters),
  });
};

export const useDeliveryAnalyticsRegions = (filters: DeliveryAnalyticsFilters) => {
  return useQuery({
    queryKey: DELIVERY_ANALYTICS_QUERY_KEYS.regions(filters),
    queryFn: () => deliveryAnalyticsApi.getRegions(filters),
  });
};

export const useDeliveryAnalyticsTrends = (filters: DeliveryAnalyticsFilters, granularity: 'DAY' | 'WEEK' | 'MONTH' = 'DAY') => {
  return useQuery({
    queryKey: DELIVERY_ANALYTICS_QUERY_KEYS.trends(filters, granularity),
    queryFn: () => deliveryAnalyticsApi.getTrends(filters, granularity),
  });
};
