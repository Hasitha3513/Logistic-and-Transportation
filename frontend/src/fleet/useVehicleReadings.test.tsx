import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OfflineSyncStorageError } from '../features/offlineSync/errors';
import { useCorrectVehicleReading, useRecordManualReading, useResetVehicleMeter } from './useVehicleReadings';

const mocks = vi.hoisted(() => ({
  enqueue: vi.fn(),
  syncNow: vi.fn(() => Promise.resolve()),
  register: vi.fn(() => () => undefined),
  post: vi.fn(),
}));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: { id: 'current-user-id' } }),
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

vi.mock('../api/client', () => ({ api: { post: mocks.post } }));

function wrapper({ children }: PropsWithChildren) {
  return <QueryClientProvider client={new QueryClient()}>{children}</QueryClientProvider>;
}

describe('vehicle reading mutations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.enqueue.mockResolvedValue({ operationId: 'operation-1', status: 'PENDING' });
    mocks.post.mockResolvedValue({ data: {} });
  });

  it('always queues a manual reading for the current authenticated owner', async () => {
    const { result } = renderHook(() => useRecordManualReading('vehicle-1'), { wrapper });
    await act(async () => {
      await result.current.mutateAsync({
        readingType: 'ODOMETER', value: 123.456,
        recordedAt: '2026-08-20T10:00:00+05:30', notes: ' checked ',
      });
    });

    expect(mocks.enqueue).toHaveBeenCalledWith({
      ownerUserId: 'current-user-id', operationType: 'VEHICLE_READING_RECORD',
      aggregateType: 'VEHICLE', aggregateId: 'vehicle-1',
      payload: {
        readingType: 'ODOMETER', value: 123.456,
        recordedAt: '2026-08-20T10:00:00+05:30', notes: 'checked',
      },
    });
    expect(mocks.syncNow).toHaveBeenCalled();
    expect(mocks.post).not.toHaveBeenCalled();
  });

  it('keeps correction and meter reset on their direct online endpoints', async () => {
    const correction = renderHook(() => useCorrectVehicleReading('vehicle-1'), { wrapper });
    const reset = renderHook(() => useResetVehicleMeter('vehicle-1'), { wrapper });
    await act(async () => {
      await correction.result.current.mutateAsync({
        readingId: 'reading-1',
        payload: { value: 10, reason: 'fix', recordedAt: '2026-08-20T10:00:00Z' },
      });
      await reset.result.current.mutateAsync({
        readingType: 'ODOMETER', newMeterValue: 0,
        effectiveAt: '2026-08-20T10:00:00Z', reason: 'replacement',
      });
    });

    expect(mocks.post).toHaveBeenCalledWith('/vehicles/vehicle-1/readings/reading-1/correct', expect.anything());
    expect(mocks.post).toHaveBeenCalledWith('/vehicles/vehicle-1/meter-resets', expect.anything());
    expect(mocks.enqueue).not.toHaveBeenCalled();
  });

  it('reports local capacity without falling back to the direct API', async () => {
    mocks.enqueue.mockRejectedValueOnce(new OfflineSyncStorageError(
      'OFFLINE_SYNC_LOCAL_CAPACITY_EXCEEDED', 'capacity reached',
    ));
    const { result } = renderHook(() => useRecordManualReading('vehicle-1'), { wrapper });

    await expect(result.current.mutateAsync({
      readingType: 'ODOMETER', value: 123, recordedAt: '2026-08-20T10:00:00Z',
    })).rejects.toThrow('Offline queue is full');
    expect(mocks.post).not.toHaveBeenCalled();
  });
});
