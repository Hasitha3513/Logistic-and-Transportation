import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type { DriverMedicalRecord, DriverMedicalRecordRequest } from './types';

export function useDriverMedicalRecords(driverId?: string) {
  return useQuery<DriverMedicalRecord[]>({
    queryKey: ['driver-medical-records', driverId],
    queryFn: async () => {
      if (!driverId) return [];
      const res = await api.get<DriverMedicalRecord[]>(`/drivers/${driverId}/medical-records`);
      return res.data;
    },
    enabled: Boolean(driverId),
  });
}

export function useCreateDriverMedicalRecord(driverId?: string) {
  const queryClient = useQueryClient();
  return useMutation<DriverMedicalRecord, Error, DriverMedicalRecordRequest>({
    mutationFn: async (payload) => {
      const res = await api.post<DriverMedicalRecord>(`/drivers/${driverId}/medical-records`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driver-medical-records', driverId] });
      queryClient.invalidateQueries({ queryKey: ['driver-availability'] });
    },
  });
}
