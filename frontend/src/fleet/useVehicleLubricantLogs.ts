import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type { LubricantLog, LubricantLogRequest } from './types';

export function useVehicleLubricantLogs(vehicleId?: string) {
  return useQuery<LubricantLog[]>({
    queryKey: ['vehicle-lubricant-logs', vehicleId],
    queryFn: async () => {
      if (!vehicleId) return [];
      const res = await api.get<LubricantLog[]>(`/vehicles/${vehicleId}/lubricant-logs`);
      return res.data;
    },
    enabled: Boolean(vehicleId),
  });
}

export function useCreateVehicleLubricantLog(vehicleId?: string) {
  const queryClient = useQueryClient();
  return useMutation<LubricantLog, Error, LubricantLogRequest>({
    mutationFn: async (payload) => {
      const res = await api.post<LubricantLog>(`/vehicles/${vehicleId}/lubricant-logs`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicle-lubricant-logs', vehicleId] });
      queryClient.invalidateQueries({ queryKey: ['vehicles'] });
      queryClient.invalidateQueries({ queryKey: ['vehicle-readings', vehicleId] });
    },
  });
}
