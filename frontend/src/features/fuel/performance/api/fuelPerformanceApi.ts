import { api } from '../../../../api/client';
import type {
  DriverPerformance, FuelPerformanceFilters, Page, Summary, Trend, VehiclePerformance,
} from '../types/fuelPerformance';

const params = (filters: FuelPerformanceFilters) => filters;

export const fuelPerformanceApi = {
  summary: async (filters: FuelPerformanceFilters) =>
    (await api.get<Summary>('/v1/fuel/performance/summary', { params: params(filters) })).data,
  vehicles: async (filters: FuelPerformanceFilters, page = 0, size = 20) =>
    (await api.get<Page<VehiclePerformance>>('/v1/fuel/performance/vehicles', {
      params: { ...params(filters), page, size },
    })).data,
  drivers: async (filters: FuelPerformanceFilters, page = 0, size = 20) =>
    (await api.get<Page<DriverPerformance>>('/v1/fuel/performance/drivers', {
      params: { ...params(filters), page, size },
    })).data,
  trends: async (filters: FuelPerformanceFilters) =>
    (await api.get<Trend[]>('/v1/fuel/performance/trends', { params: params(filters) })).data,
};
