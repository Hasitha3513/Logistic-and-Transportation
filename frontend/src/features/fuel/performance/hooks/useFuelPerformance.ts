import { useQuery } from '@tanstack/react-query';
import { fuelPerformanceApi } from '../api/fuelPerformanceApi';
import type { FuelPerformanceFilters } from '../types/fuelPerformance';

export const FUEL_PERFORMANCE_KEYS = {
  all: ['fuel-performance'] as const,
  summary: (filters: FuelPerformanceFilters) => [...FUEL_PERFORMANCE_KEYS.all, 'summary', filters] as const,
  vehicles: (filters: FuelPerformanceFilters) => [...FUEL_PERFORMANCE_KEYS.all, 'vehicles', filters] as const,
  drivers: (filters: FuelPerformanceFilters) => [...FUEL_PERFORMANCE_KEYS.all, 'drivers', filters] as const,
  trends: (filters: FuelPerformanceFilters) => [...FUEL_PERFORMANCE_KEYS.all, 'trends', filters] as const,
};

export const useFuelPerformance = (filters: FuelPerformanceFilters) => ({
  summary: useQuery({ queryKey: FUEL_PERFORMANCE_KEYS.summary(filters), queryFn: () => fuelPerformanceApi.summary(filters) }),
  vehicles: useQuery({ queryKey: FUEL_PERFORMANCE_KEYS.vehicles(filters), queryFn: () => fuelPerformanceApi.vehicles(filters) }),
  drivers: useQuery({ queryKey: FUEL_PERFORMANCE_KEYS.drivers(filters), queryFn: () => fuelPerformanceApi.drivers(filters) }),
  trends: useQuery({ queryKey: FUEL_PERFORMANCE_KEYS.trends(filters), queryFn: () => fuelPerformanceApi.trends(filters) }),
});
