import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type {
  DriverDrugTest,
  DriverDrugTestRequest,
  DriverDrugTestResultRequest,
} from './types';

export function useDriverDrugTests(driverId?: string) {
  return useQuery<DriverDrugTest[]>({
    queryKey: ['driver-drug-tests', driverId],
    queryFn: async () => {
      if (!driverId) return [];
      const res = await api.get<DriverDrugTest[]>(`/drivers/${driverId}/drug-tests`);
      return res.data;
    },
    enabled: Boolean(driverId),
  });
}

export function useScheduleDriverDrugTest(driverId?: string) {
  const queryClient = useQueryClient();
  return useMutation<DriverDrugTest, Error, DriverDrugTestRequest>({
    mutationFn: async (payload) => {
      const res = await api.post<DriverDrugTest>(`/drivers/${driverId}/drug-tests`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driver-drug-tests', driverId] });
      queryClient.invalidateQueries({ queryKey: ['driver-availability'] });
    },
  });
}

export function useRecordDrugTestResult(driverId?: string) {
  const queryClient = useQueryClient();
  return useMutation<DriverDrugTest, Error, { testId: string; payload: DriverDrugTestResultRequest }>({
    mutationFn: async ({ testId, payload }) => {
      const res = await api.post<DriverDrugTest>(`/drivers/${driverId}/drug-tests/${testId}/result`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driver-drug-tests', driverId] });
      queryClient.invalidateQueries({ queryKey: ['driver-availability'] });
    },
  });
}

export function useClearReturnToDuty(driverId?: string) {
  const queryClient = useQueryClient();
  return useMutation<DriverDrugTest, Error, { testId: string; remarks?: string }>({
    mutationFn: async ({ testId, remarks }) => {
      const res = await api.post<DriverDrugTest>(`/drivers/${driverId}/drug-tests/${testId}/return-to-duty-clear`, {
        remarks,
      });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driver-drug-tests', driverId] });
      queryClient.invalidateQueries({ queryKey: ['driver-availability'] });
    },
  });
}
