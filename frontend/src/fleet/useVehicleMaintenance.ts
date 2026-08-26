import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type { MaintenanceSchedule, MaintenanceSchedulePatchRequest, MaintenanceScheduleRequest } from './types';

export function useVehicleMaintenanceSchedules(vehicleId?: string) {
  return useQuery({
    queryKey: ['vehicle-maintenance', vehicleId],
    queryFn: async () => {
      if (!vehicleId) return [];
      const response = await api.get<MaintenanceSchedule[]>(`/vehicles/${vehicleId}/maintenance-schedules`);
      return response.data;
    },
    enabled: Boolean(vehicleId),
  });
}

export function useCreateMaintenanceSchedule(vehicleId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: MaintenanceScheduleRequest) => {
      const response = await api.post<MaintenanceSchedule>(`/vehicles/${vehicleId}/maintenance-schedules`, payload);
      return response.data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['vehicle-maintenance', vehicleId] });
      await queryClient.invalidateQueries({ queryKey: ['vehicles-page'] });
    },
  });
}

export function useUpdateMaintenanceSchedule(vehicleId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ scheduleId, payload }: { scheduleId: string; payload: MaintenanceSchedulePatchRequest }) => {
      const response = await api.patch<MaintenanceSchedule>(
        `/vehicles/${vehicleId}/maintenance-schedules/${scheduleId}`,
        payload
      );
      return response.data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['vehicle-maintenance', vehicleId] });
      await queryClient.invalidateQueries({ queryKey: ['vehicles-page'] });
    },
  });
}

export function useCancelMaintenanceSchedule(vehicleId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ scheduleId, remarks }: { scheduleId: string; remarks?: string }) => {
      const response = await api.post<MaintenanceSchedule>(
        `/vehicles/${vehicleId}/maintenance-schedules/${scheduleId}/cancel`,
        { remarks }
      );
      return response.data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['vehicle-maintenance', vehicleId] });
      await queryClient.invalidateQueries({ queryKey: ['vehicles-page'] });
    },
  });
}

export function useCompleteMaintenanceSchedule(vehicleId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ scheduleId, remarks }: { scheduleId: string; remarks?: string }) => {
      const response = await api.post<MaintenanceSchedule>(
        `/vehicles/${vehicleId}/maintenance-schedules/${scheduleId}/complete`,
        { remarks }
      );
      return response.data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['vehicle-maintenance', vehicleId] });
      await queryClient.invalidateQueries({ queryKey: ['vehicles-page'] });
    },
  });
}
