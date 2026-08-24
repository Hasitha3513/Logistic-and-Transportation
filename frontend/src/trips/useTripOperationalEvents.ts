import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { OfflineSyncStorageError } from '../features/offlineSync/errors';
import { useOfflineSync } from '../features/offlineSync/OfflineSyncProvider';
import type { OfflineOperation, OfflineOperationInput } from '../features/offlineSync/types';
import type {
  TripCheckpointRequest,
  TripDelayRequest,
  TripIncidentRequest,
  TripOperationalEvent,
} from './types';

type TripOperationalOfflineOperation = Exclude<
  OfflineOperation,
  { operationType: 'VEHICLE_READING_RECORD' }
>;

export function useTripOperationalEvents(tripId?: string) {
  const queryClient = useQueryClient();
  const { registerPostApply } = useOfflineSync();
  useEffect(() => {
    const invalidate = async (operation: OfflineOperation) => {
      if (operation.aggregateId !== tripId) return;
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trip-operational-events', tripId] }),
        queryClient.invalidateQueries({ queryKey: ['trip-history', tripId] }),
        queryClient.invalidateQueries({ queryKey: ['trip', tripId] }),
        queryClient.invalidateQueries({ queryKey: ['trips'] }),
      ]);
    };
    const unregister = [
      registerPostApply('TRIP_CHECKPOINT_RECORD', invalidate),
      registerPostApply('TRIP_DELAY_RECORD', invalidate),
      registerPostApply('TRIP_INCIDENT_RECORD', invalidate),
    ];
    return () => unregister.forEach((callback) => callback());
  }, [queryClient, registerPostApply, tripId]);

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
  return useQueueTripEvent(tripId, (ownerUserId, payload: TripCheckpointRequest) => ({
    ownerUserId,
    operationType: 'TRIP_CHECKPOINT_RECORD',
    aggregateType: 'TRIP',
    aggregateId: tripId,
    payload: {
      checkpointType: payload.checkpointType,
      occurredAt: requiredOccurredAt(payload.occurredAt),
      ...optionalFields(payload),
    },
  }));
}

export function useRecordTripDelay(tripId: string) {
  return useQueueTripEvent(tripId, (ownerUserId, payload: TripDelayRequest) => ({
    ownerUserId,
    operationType: 'TRIP_DELAY_RECORD',
    aggregateType: 'TRIP',
    aggregateId: tripId,
    payload: {
      delayMinutes: payload.delayMinutes,
      reason: payload.reason.trim(),
      occurredAt: requiredOccurredAt(payload.occurredAt),
      ...optionalFields(payload),
    },
  }));
}

export function useRecordTripIncident(tripId: string) {
  return useQueueTripEvent(tripId, (ownerUserId, payload: TripIncidentRequest) => ({
    ownerUserId,
    operationType: 'TRIP_INCIDENT_RECORD',
    aggregateType: 'TRIP',
    aggregateId: tripId,
    payload: {
      incidentSeverity: payload.incidentSeverity,
      description: payload.description.trim(),
      occurredAt: requiredOccurredAt(payload.occurredAt),
      ...optionalFields(payload),
    },
  }));
}

export function useLocalTripOperationalEvents(tripId?: string) {
  const { user } = useAuth();
  const { getOperationsForAggregate, operationsRevision } = useOfflineSync();
  return useQuery({
    queryKey: ['offline-trip-operational-events', user?.id, tripId, operationsRevision],
    queryFn: async () => {
      if (!tripId || !user?.id) return [];
      return (await getOperationsForAggregate('TRIP', tripId)).filter(
        (operation): operation is TripOperationalOfflineOperation =>
        operation.operationType === 'TRIP_CHECKPOINT_RECORD'
        || operation.operationType === 'TRIP_DELAY_RECORD'
        || operation.operationType === 'TRIP_INCIDENT_RECORD',
      );
    },
    enabled: Boolean(tripId && user?.id),
  });
}

function useQueueTripEvent<Payload>(
  tripId: string,
  command: (ownerUserId: string, payload: Payload) => OfflineOperationInput,
) {
  const { user } = useAuth();
  const { enqueueOperation, syncNow } = useOfflineSync();
  return useMutation({
    networkMode: 'always',
    mutationFn: async (payload: Payload) => {
      if (!tripId || !user?.id) throw new Error('A signed-in user and trip are required');
      try {
        const operation = await enqueueOperation(command(user.id, payload));
        void syncNow();
        return operation;
      } catch (error: unknown) {
        if (error instanceof OfflineSyncStorageError
            && error.code === 'OFFLINE_SYNC_LOCAL_CAPACITY_EXCEEDED') {
          throw new Error('Offline queue is full. Sync or resolve existing offline items before adding another event.');
        }
        throw error;
      }
    },
  });
}

function requiredOccurredAt(value?: string): string {
  if (!value) throw new Error('Occurred time is required');
  return value;
}

function optionalFields(payload: {
  locationId?: string | null;
  locationDescription?: string | null;
  remarks?: string | null;
}) {
  return {
    ...(payload.locationId ? { locationId: payload.locationId } : {}),
    ...(payload.locationDescription?.trim() ? { locationDescription: payload.locationDescription.trim() } : {}),
    ...(payload.remarks?.trim() ? { remarks: payload.remarks.trim() } : {}),
  };
}
