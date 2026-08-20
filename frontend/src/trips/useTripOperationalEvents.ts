import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type {
  TripCheckpointRequest,
  TripDelayRequest,
  TripIncidentRequest,
  TripOperationalEvent,
} from './types';

export function useTripOperationalEvents(tripId?: string) {
  return useQuery({
    queryKey: ['trip-operational-events', tripId],
    queryFn: async () => {
      if (!tripId) return [];
      const response = await api.get<TripOperationalEvent[]>(`/trips/${tripId}/operational-events`);
      return response.data;
    },
    enabled: Boolean(tripId),
  });
}

export function useRecordTripCheckpoint(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: TripCheckpointRequest) => {
      const response = await api.post<TripOperationalEvent>(`/trips/${tripId}/checkpoints`, payload);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trip-operational-events', tripId] });
      queryClient.invalidateQueries({ queryKey: ['trip-history', tripId] });
      queryClient.invalidateQueries({ queryKey: ['trip', tripId] });
      queryClient.invalidateQueries({ queryKey: ['trips'] });
    },
  });
}

export function useRecordTripDelay(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: TripDelayRequest) => {
      const response = await api.post<TripOperationalEvent>(`/trips/${tripId}/delays`, payload);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trip-operational-events', tripId] });
      queryClient.invalidateQueries({ queryKey: ['trip-history', tripId] });
      queryClient.invalidateQueries({ queryKey: ['trip', tripId] });
      queryClient.invalidateQueries({ queryKey: ['trips'] });
    },
  });
}

export function useRecordTripIncident(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: TripIncidentRequest) => {
      const response = await api.post<TripOperationalEvent>(`/trips/${tripId}/incidents`, payload);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trip-operational-events', tripId] });
      queryClient.invalidateQueries({ queryKey: ['trip-history', tripId] });
      queryClient.invalidateQueries({ queryKey: ['trip', tripId] });
      queryClient.invalidateQueries({ queryKey: ['trips'] });
    },
  });
}
