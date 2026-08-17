import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type {
  LatestVehicleReadings,
  PageResponse,
  RecordCorrectionRequest,
  RecordManualReadingRequest,
  RecordMeterResetRequest,
  VehicleMeterReset,
  VehicleMileageSummary,
  VehicleReading,
  VehicleReadingSourceType,
  VehicleReadingType,
} from './types';

interface SearchReadingsParams {
  readingType?: VehicleReadingType;
  sourceType?: VehicleReadingSourceType;
  from?: string;
  to?: string;
  page?: number;
  limit?: number;
}

export function useVehicleReadings(vehicleId?: string, params?: SearchReadingsParams) {
  return useQuery({
    queryKey: ['vehicle-readings', vehicleId, params],
    queryFn: async () => {
      if (!vehicleId) return { content: [], page: 0, limit: 20, totalElements: 0, totalPages: 0 };
      const { data } = await api.get<PageResponse<VehicleReading>>(`/vehicles/${vehicleId}/readings`, {
        params: {
          readingType: params?.readingType,
          sourceType: params?.sourceType,
          from: params?.from,
          to: params?.to,
          page: params?.page ?? 0,
          limit: params?.limit ?? 20,
        },
      });
      return data;
    },
    enabled: Boolean(vehicleId),
  });
}

export function useLatestVehicleReadings(vehicleId?: string) {
  return useQuery({
    queryKey: ['vehicle-readings-latest', vehicleId],
    queryFn: async () => {
      if (!vehicleId) return null;
      const { data } = await api.get<LatestVehicleReadings>(`/vehicles/${vehicleId}/readings/latest`);
      return data;
    },
    enabled: Boolean(vehicleId),
  });
}

export function useVehicleMeterResets(vehicleId?: string) {
  return useQuery({
    queryKey: ['vehicle-meter-resets', vehicleId],
    queryFn: async () => {
      if (!vehicleId) return [];
      const { data } = await api.get<VehicleMeterReset[]>(`/vehicles/${vehicleId}/meter-resets`);
      return data;
    },
    enabled: Boolean(vehicleId),
  });
}

export function useVehicleMileage(vehicleId?: string, from?: string, to?: string) {
  return useQuery({
    queryKey: ['vehicle-mileage', vehicleId, from, to],
    queryFn: async () => {
      if (!vehicleId) return null;
      const { data } = await api.get<VehicleMileageSummary>(`/vehicles/${vehicleId}/mileage`, {
        params: { from, to },
      });
      return data;
    },
    enabled: Boolean(vehicleId),
  });
}

export function useRecordManualReading(vehicleId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: RecordManualReadingRequest) => {
      const { data } = await api.post<VehicleReading>(`/vehicles/${vehicleId}/readings`, payload);
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['vehicle-readings', vehicleId] });
      void queryClient.invalidateQueries({ queryKey: ['vehicle-readings-latest', vehicleId] });
      void queryClient.invalidateQueries({ queryKey: ['vehicle-mileage', vehicleId] });
      void queryClient.invalidateQueries({ queryKey: ['vehicles-page'] });
    },
  });
}

export function useCorrectVehicleReading(vehicleId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ readingId, payload }: { readingId: string; payload: RecordCorrectionRequest }) => {
      const { data } = await api.post<VehicleReading>(`/vehicles/${vehicleId}/readings/${readingId}/correct`, payload);
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['vehicle-readings', vehicleId] });
      void queryClient.invalidateQueries({ queryKey: ['vehicle-readings-latest', vehicleId] });
      void queryClient.invalidateQueries({ queryKey: ['vehicle-mileage', vehicleId] });
      void queryClient.invalidateQueries({ queryKey: ['vehicles-page'] });
    },
  });
}

export function useResetVehicleMeter(vehicleId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: RecordMeterResetRequest) => {
      const { data } = await api.post<VehicleMeterReset>(`/vehicles/${vehicleId}/meter-resets`, payload);
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['vehicle-meter-resets', vehicleId] });
      void queryClient.invalidateQueries({ queryKey: ['vehicle-readings', vehicleId] });
      void queryClient.invalidateQueries({ queryKey: ['vehicle-readings-latest', vehicleId] });
      void queryClient.invalidateQueries({ queryKey: ['vehicle-mileage', vehicleId] });
      void queryClient.invalidateQueries({ queryKey: ['vehicles-page'] });
    },
  });
}