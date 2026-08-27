import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type {
  DriverViolation,
  DriverViolationRequest,
  PayFineRequest,
  WaiveFineRequest,
} from './types';

export function useDriverViolations(driverId?: string) {
  return useQuery<DriverViolation[]>({
    queryKey: ['driver-violations', driverId],
    queryFn: async () => {
      if (!driverId) return [];
      const res = await api.get<DriverViolation[]>(`/drivers/${driverId}/violations`);
      return res.data;
    },
    enabled: Boolean(driverId),
  });
}

export function useRecordDriverViolation(driverId?: string) {
  const queryClient = useQueryClient();
  return useMutation<DriverViolation, Error, DriverViolationRequest>({
    mutationFn: async (payload) => {
      const res = await api.post<DriverViolation>(`/drivers/${driverId}/violations`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driver-violations', driverId] });
      queryClient.invalidateQueries({ queryKey: ['driver-performance', driverId] });
    },
  });
}

export function usePayDriverViolation(driverId?: string) {
  const queryClient = useQueryClient();
  return useMutation<DriverViolation, Error, { violationId: string; payload?: PayFineRequest }>({
    mutationFn: async ({ violationId, payload }) => {
      const res = await api.post<DriverViolation>(
        `/drivers/${driverId}/violations/${violationId}/pay`,
        payload || {}
      );
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driver-violations', driverId] });
      queryClient.invalidateQueries({ queryKey: ['driver-performance', driverId] });
    },
  });
}

export function useWaiveDriverViolation(driverId?: string) {
  const queryClient = useQueryClient();
  return useMutation<DriverViolation, Error, { violationId: string; payload: WaiveFineRequest }>({
    mutationFn: async ({ violationId, payload }) => {
      const res = await api.post<DriverViolation>(
        `/drivers/${driverId}/violations/${violationId}/waive`,
        payload
      );
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driver-violations', driverId] });
      queryClient.invalidateQueries({ queryKey: ['driver-performance', driverId] });
    },
  });
}

export function useDisputeDriverViolation(driverId?: string) {
  const queryClient = useQueryClient();
  return useMutation<DriverViolation, Error, { violationId: string; payload: WaiveFineRequest }>({
    mutationFn: async ({ violationId, payload }) => {
      const res = await api.post<DriverViolation>(
        `/drivers/${driverId}/violations/${violationId}/dispute`,
        payload
      );
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driver-violations', driverId] });
      queryClient.invalidateQueries({ queryKey: ['driver-performance', driverId] });
    },
  });
}
