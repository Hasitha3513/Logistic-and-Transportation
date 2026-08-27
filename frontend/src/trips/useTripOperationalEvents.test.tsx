import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OfflineSyncStorageError } from '../features/offlineSync/errors';
import {
  useRecordTripCheckpoint,
  useRecordTripDelay,
  useRecordTripIncident,
  useTripOperationalEvents,
} from './useTripOperationalEvents';

const mocks = vi.hoisted(() => ({
  enqueue: vi.fn(),
  syncNow: vi.fn(() => Promise.resolve()),
  register: vi.fn<(type: string, callback: unknown) => () => void>(() => () => undefined),
  get: vi.fn(),
  post: vi.fn(),
}));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: { id: '11000000-0000-4000-8000-000000000001' } }),
}));

vi.mock('../features/offlineSync/OfflineSyncProvider', () => ({
  useOfflineSync: () => ({
    enqueueOperation: mocks.enqueue,
    syncNow: mocks.syncNow,
    registerPostApply: mocks.register,
    getOperationsForAggregate: vi.fn(),
    operationsRevision: 0,
  }),
}));

vi.mock('../api/client', () => ({ api: { get: mocks.get, post: mocks.post } }));

function wrapper({ children }: PropsWithChildren) {
  return <QueryClientProvider client={new QueryClient()}>{children}</QueryClientProvider>;
}

describe('trip operational-event mutations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.enqueue.mockResolvedValue({ operationId: 'operation-1', status: 'PENDING' });
    mocks.get.mockResolvedValue({ data: [] });
  });

  it('queues checkpoint, delay, and incident operations with exact version-one payloads', async () => {
    const checkpoint = renderHook(() => useRecordTripCheckpoint('trip-1'), { wrapper });
    const delay = renderHook(() => useRecordTripDelay('trip-1'), { wrapper });
    const incident = renderHook(() => useRecordTripIncident('trip-1'), { wrapper });

    await act(async () => {
      await checkpoint.result.current.mutateAsync({
        checkpointType: 'PICKUP', occurredAt: '2026-08-20T10:00:00Z',
        locationDescription: ' Dock 4 ', remarks: ' Loaded ',
      });
      await delay.result.current.mutateAsync({
        delayMinutes: 25, reason: ' Traffic ', occurredAt: '2026-08-20T11:00:00Z',
      });
      await incident.result.current.mutateAsync({
        incidentSeverity: 'MEDIUM', description: ' Tire puncture ',
        occurredAt: '2026-08-20T12:00:00Z', remarks: ' Spare fitted ',
      });
    });

    const common = {
      ownerUserId: '11000000-0000-4000-8000-000000000001',
      aggregateType: 'TRIP', aggregateId: 'trip-1',
    };
    expect(mocks.enqueue).toHaveBeenNthCalledWith(1, {
      ...common, operationType: 'TRIP_CHECKPOINT_RECORD',
      payload: {
        checkpointType: 'PICKUP', occurredAt: '2026-08-20T10:00:00Z',
        locationDescription: 'Dock 4', remarks: 'Loaded',
      },
    });
    expect(mocks.enqueue).toHaveBeenNthCalledWith(2, {
      ...common, operationType: 'TRIP_DELAY_RECORD',
      payload: { delayMinutes: 25, reason: 'Traffic', occurredAt: '2026-08-20T11:00:00Z' },
    });
    expect(mocks.enqueue).toHaveBeenNthCalledWith(3, {
      ...common, operationType: 'TRIP_INCIDENT_RECORD',
      payload: {
        incidentSeverity: 'MEDIUM', description: 'Tire puncture',
        occurredAt: '2026-08-20T12:00:00Z', remarks: 'Spare fitted',
      },
    });
    expect(mocks.syncNow).toHaveBeenCalledTimes(3);
    expect(mocks.post).not.toHaveBeenCalled();
  });

  it('reports local capacity without falling back to direct operational endpoints', async () => {
    mocks.enqueue.mockRejectedValueOnce(new OfflineSyncStorageError(
      'OFFLINE_SYNC_LOCAL_CAPACITY_EXCEEDED', 'capacity reached',
    ));
    const checkpoint = renderHook(() => useRecordTripCheckpoint('trip-1'), { wrapper });

    await expect(checkpoint.result.current.mutateAsync({
      checkpointType: 'DEPARTURE', occurredAt: '2026-08-20T10:00:00Z',
    })).rejects.toThrow('Offline queue is full');
    expect(mocks.post).not.toHaveBeenCalled();
  });

  it('registers all Trip operation types and invalidates server-confirmed views after apply', async () => {
    const queryClient = new QueryClient();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue();
    const queryWrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    renderHook(() => useTripOperationalEvents('trip-1'), { wrapper: queryWrapper });

    expect(mocks.register.mock.calls.map(([type]) => type)).toEqual([
      'TRIP_CHECKPOINT_RECORD', 'TRIP_DELAY_RECORD', 'TRIP_INCIDENT_RECORD',
    ]);
    const postApply = mocks.register.mock.calls[0][1] as (
      operation: { aggregateId: string },
    ) => Promise<void>;
    await act(async () => postApply({ aggregateId: 'trip-1' }));

    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['trip-operational-events', 'trip-1'] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['trip-history', 'trip-1'] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['trip', 'trip-1'] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['trips'] });
  });
});
