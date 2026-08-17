import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type {
  CorrectionRequest,
  LatestReadingsResponse,
  ManualReadingRequest,
  MeterResetRequest,
  MeterResetResponse,
  PageResult,
  TripDistanceSummaryResponse,
  VehicleMileageSummaryResponse,
  VehicleReadingResponse,
  VehicleReadingType,
} from './types';

export function useVehicleReadings(vehicleId?: string, typeFilter?: VehicleReadingType) {
  return useQuery({
    queryKey: ['vehicles', vehicleId, 'readings', typeFilter],
    queryFn: async () => {
      if (!vehicleId) return { content: [], page: 0, limit: 50, totalElements: 0, totalPages: 0 };
      const params: Record<string, unknown> = { limit: 50 };
      if (typeFilter) params.readingType = typeFilter;
      const res = await api.get<PageResult<VehicleReadingResponse>>(`/vehicles/${vehicleId}/readings`, { params });
      return res.data;
    },
    enabled: Boolean(vehicleId),
  });
}

export function useLatestVehicleReadings(vehicleId?: string) {
  return useQuery({
    queryKey: ['vehicles', vehicleId, 'readings', 'latest'],
    queryFn: async () => {
      if (!vehicleId) return null;
      const res = await api.get<LatestReadingsResponse>(`/vehicles/${vehicleId}/readings/latest`);
      return res.data;
    },
    enabled: Boolean(vehicleId),
  });
}

export function useVehicleMileageSummary(vehicleId?: string, from?: string, to?: string, includeSourceBreakdown = true) {
  return useQuery({
    queryKey: ['vehicles', vehicleId, 'mileage-summary', from, to, includeSourceBreakdown],
    queryFn: async () => {
      if (!vehicleId || !from || !to) return null;
      const res = await api.get<VehicleMileageSummaryResponse>(`/vehicles/${vehicleId}/mileage-summary`, {
        params: { from, to, includeSourceBreakdown },
      });
      return res.data;
    },
    enabled: Boolean(vehicleId && from && to),
  });
}

export function useTripDistance(tripId?: string) {
  return useQuery({
    queryKey: ['trips', tripId, 'distance'],
    queryFn: async () => {
      if (!tripId) return null;
      const res = await api.get<TripDistanceSummaryResponse>(`/trips/${tripId}/distance`);
      return res.data;
    },
    enabled: Boolean(tripId),
  });
}

export function useVehicleMeterResets(vehicleId?: string) {
  return useQuery({
    queryKey: ['vehicles', vehicleId, 'meter-resets'],
    queryFn: async () => {
      if (!vehicleId) return [];
      const res = await api.get<MeterResetResponse[]>(`/vehicles/${vehicleId}/meter-resets`);
      return res.data;
    },
    enabled: Boolean(vehicleId),
  });
}

export function useRecordManualReading(vehicleId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: ManualReadingRequest) => {
      const res = await api.post<VehicleReadingResponse>(`/vehicles/${vehicleId}/readings`, payload);
      return res.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['vehicles', vehicleId, 'readings'] });
      void queryClient.invalidateQueries({ queryKey: ['vehicles', vehicleId, 'mileage-summary'] });
      void queryClient.invalidateQueries({ queryKey: ['vehicles-page'] });
    },
  });
}

export function useCorrectReading(vehicleId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ readingId, payload }: { readingId: string; payload: CorrectionRequest }) => {
      const res = await api.post<VehicleReadingResponse>(`/vehicles/${vehicleId}/readings/${readingId}/correct`, payload);
      return res.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['vehicles', vehicleId, 'readings'] });
      void queryClient.invalidateQueries({ queryKey: ['vehicles', vehicleId, 'mileage-summary'] });
      void queryClient.invalidateQueries({ queryKey: ['vehicles-page'] });
    },
  });
}

export function useResetVehicleMeter(vehicleId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: MeterResetRequest) => {
      const res = await api.post<MeterResetResponse>(`/vehicles/${vehicleId}/meter-resets`, payload);
      return res.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['vehicles', vehicleId, 'readings'] });
      void queryClient.invalidateQueries({ queryKey: ['vehicles', vehicleId, 'meter-resets'] });
      void queryClient.invalidateQueries({ queryKey: ['vehicles', vehicleId, 'mileage-summary'] });
      void queryClient.invalidateQueries({ queryKey: ['vehicles-page'] });
    },
  });
}
