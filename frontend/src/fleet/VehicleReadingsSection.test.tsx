import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { describe, expect, it, vi } from 'vitest';
import type { OfflineOperation } from '../features/offlineSync/types';
import VehicleReadingsSection from './VehicleReadingsSection';

const offline = vi.hoisted(() => ({
  enqueueOperation: vi.fn(),
  syncNow: vi.fn(() => Promise.resolve()),
  registerPostApply: vi.fn(() => () => undefined),
  getOperationsForAggregate: vi.fn<() => Promise<OfflineOperation[]>>(() => Promise.resolve([])),
}));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: () => true,
    user: { id: 'user-1', username: 'fuel.manager' },
  }),
}));

vi.mock('../features/offlineSync/OfflineSyncProvider', () => ({
  useOfflineSync: () => ({
    ...offline,
    operationsRevision: 0,
    onlineHint: true,
    backendReachable: true,
    syncing: false,
    authPaused: false,
  }),
}));

vi.mock('../api/client', () => ({
  api: {
    get: vi.fn((url: string) => {
      if (url.includes('/latest')) {
        return Promise.resolve({
          data: {
            vehicleId: 'test-vehicle-id',
            odometer: { value: 12500, unit: 'KILOMETERS', meterEpoch: 0, recordedAt: '2026-08-16T10:00:00Z' },
            engineHours: { value: 450, unit: 'HOURS', meterEpoch: 0, recordedAt: '2026-08-16T10:00:00Z' },
          },
        });
      }
      if (url.includes('/mileage')) {
        return Promise.resolve({
          data: {
            vehicleId: 'test-vehicle-id',
            distanceTravelledKm: 500,
            engineHoursUsed: 20,
            meterResetCount: 0,
            coverageStatus: 'COMPLETE',
            abnormalDetected: false,
          },
        });
      }
      if (url.includes('/meter-resets')) {
        return Promise.resolve({ data: [] });
      }
      if (url.includes('/readings')) {
        return Promise.resolve({
          data: {
            content: [
              {
                id: 'r-1',
                vehicleId: 'test-vehicle-id',
                readingType: 'ODOMETER',
                value: 12500,
                unit: 'KILOMETERS',
                meterEpoch: 0,
                sourceType: 'MANUAL',
                recordedAt: '2026-08-16T10:00:00Z',
              },
            ],
            totalElements: 1,
            totalPages: 1,
            page: 0,
            limit: 10,
          },
        });
      }
      return Promise.resolve({ data: {} });
    }),
    post: vi.fn(() => Promise.resolve({ data: {} })),
  },
}));

describe('VehicleReadingsSection', () => {
  it('renders snapshots and mileage statistics', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <AntApp>
          <VehicleReadingsSection vehicleId="test-vehicle-id" />
        </AntApp>
      </QueryClientProvider>
    );

    expect(screen.getByText('Vehicle Mileage & Readings')).toBeDefined();
    expect(screen.getByText('Current Odometer')).toBeDefined();
    expect(screen.getByText('Current Engine Hours')).toBeDefined();
    expect(screen.getByText('Record Reading')).toBeDefined();
    expect(screen.getByText('Reset Meter')).toBeDefined();
  });

  it('shows pending local readings without duplicating synced rows', async () => {
    offline.getOperationsForAggregate.mockResolvedValueOnce([
      {
        operationId: 'pending-1', operationVersion: 1, operationType: 'VEHICLE_READING_RECORD',
        aggregateType: 'VEHICLE', aggregateId: 'test-vehicle-id', ownerUserId: 'user-1',
        payload: { readingType: 'ODOMETER', value: 12600, recordedAt: '2026-08-16T11:00:00Z' },
        status: 'PENDING', createdAt: '2026-08-16T11:00:00Z', updatedAt: '2026-08-16T11:00:00Z',
        clientCreatedAt: '2026-08-16T11:00:00Z', clientUpdatedAt: '2026-08-16T11:00:00Z',
        clientInstanceId: 'client-1', idempotencyKey: 'pending-1', baseVersion: null, attemptCount: 0,
      },
      {
        operationId: 'synced-1', operationVersion: 1, operationType: 'VEHICLE_READING_RECORD',
        aggregateType: 'VEHICLE', aggregateId: 'test-vehicle-id', ownerUserId: 'user-1',
        payload: { readingType: 'ODOMETER', value: 12500, recordedAt: '2026-08-16T10:00:00Z' },
        status: 'SYNCED', createdAt: '2026-08-16T10:00:00Z', updatedAt: '2026-08-16T10:00:00Z',
        clientCreatedAt: '2026-08-16T10:00:00Z', clientUpdatedAt: '2026-08-16T10:00:00Z',
        clientInstanceId: 'client-1', idempotencyKey: 'synced-1', baseVersion: null, attemptCount: 1,
      },
    ]);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AntApp><VehicleReadingsSection vehicleId="test-vehicle-id" /></AntApp>
      </QueryClientProvider>,
    );

    expect(await screen.findByText('Offline reading queue')).toBeDefined();
    expect(screen.getByText('Pending')).toBeDefined();
    expect(screen.queryByText('Synced')).toBeNull();
  });

  it('shows safe failed and conflict reasons from the local queue', async () => {
    const base = {
      operationVersion: 1 as const, operationType: 'VEHICLE_READING_RECORD' as const,
      aggregateType: 'VEHICLE' as const, aggregateId: 'test-vehicle-id', ownerUserId: 'user-1',
      payload: { readingType: 'ODOMETER' as const, value: 12600, recordedAt: '2026-08-16T11:00:00Z' },
      createdAt: '2026-08-16T11:00:00Z', updatedAt: '2026-08-16T11:00:00Z',
      clientCreatedAt: '2026-08-16T11:00:00Z', clientUpdatedAt: '2026-08-16T11:00:00Z',
      clientInstanceId: 'client-1', baseVersion: null, attemptCount: 1,
    };
    offline.getOperationsForAggregate.mockResolvedValueOnce([
      { ...base, operationId: 'failed-1', idempotencyKey: 'failed-1', status: 'FAILED',
        lastErrorMessage: 'Payload was rejected' },
      { ...base, operationId: 'conflict-1', idempotencyKey: 'conflict-1', status: 'CONFLICT',
        lastErrorMessage: 'Chronology conflict' },
    ]);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AntApp><VehicleReadingsSection vehicleId="test-vehicle-id" /></AntApp>
      </QueryClientProvider>,
    );

    expect(await screen.findByText('Failed')).toBeDefined();
    expect(screen.getByText('Conflict')).toBeDefined();
    expect(screen.getByText('Payload was rejected')).toBeDefined();
    expect(screen.getByText('Chronology conflict')).toBeDefined();
  });
});
