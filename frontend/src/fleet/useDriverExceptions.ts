import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type { DriverException, DriverExceptionPatchRequest, DriverExceptionRequest } from './types';

export function useDriverExceptions(driverId?: string) {
  return useQuery({
    queryKey: ['driver-exceptions', driverId],
    queryFn: async () => {
      if (!driverId) return [];
      const response = await api.get<DriverException[]>(`/drivers/${driverId}/exceptions`);
      return response.data;
    },
    enabled: Boolean(driverId),
  });
}

export function useCreateDriverException(driverId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: DriverExceptionRequest) => {
      const response = await api.post<DriverException>(`/drivers/${driverId}/exceptions`, payload);
      return response.data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['driver-exceptions', driverId] });
      await queryClient.invalidateQueries({ queryKey: ['drivers-page'] });
    },
  });
}

export function useUpdateDriverException(driverId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ exceptionId, payload }: { exceptionId: string; payload: DriverExceptionPatchRequest }) => {
      const response = await api.patch<DriverException>(
        `/drivers/${driverId}/exceptions/${exceptionId}`,
        payload
      );
      return response.data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['driver-exceptions', driverId] });
      await queryClient.invalidateQueries({ queryKey: ['drivers-page'] });
    },
  });
}

export function useCancelDriverException(driverId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ exceptionId, remarks }: { exceptionId: string; remarks?: string }) => {
      const response = await api.post<DriverException>(
        `/drivers/${driverId}/exceptions/${exceptionId}/cancel`,
        { remarks }
      );
      return response.data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['driver-exceptions', driverId] });
      await queryClient.invalidateQueries({ queryKey: ['drivers-page'] });
    },
  });
}

export function useCompleteDriverException(driverId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ exceptionId, remarks }: { exceptionId: string; remarks?: string }) => {
      const response = await api.post<DriverException>(
        `/drivers/${driverId}/exceptions/${exceptionId}/complete`,
        { remarks }
      );
      return response.data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['driver-exceptions', driverId] });
      await queryClient.invalidateQueries({ queryKey: ['drivers-page'] });
    },
  });
}
