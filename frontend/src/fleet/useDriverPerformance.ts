import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import type { DriverPerformanceSummary } from './types';

export function useDriverPerformance(driverId?: string) {
  return useQuery<DriverPerformanceSummary>({
    queryKey: ['driver-performance', driverId],
    queryFn: async () => {
      if (!driverId) {
        throw new Error('Driver ID is required');
      }
      const res = await api.get<DriverPerformanceSummary>(`/drivers/${driverId}/performance`);
      return res.data;
    },
    enabled: Boolean(driverId),
  });
}
