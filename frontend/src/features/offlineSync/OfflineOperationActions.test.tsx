import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App, ConfigProvider } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OfflineOperationActions } from './OfflineOperationActions';
import type { OfflineOperation } from './types';

const retryOperation = vi.fn(() => Promise.resolve({} as OfflineOperation));
const discardOperation = vi.fn(() => Promise.resolve(true));

vi.mock('./OfflineSyncProvider', () => ({
  useOfflineSync: () => ({ retryOperation, discardOperation }),
}));

function operation(aggregateType: 'VEHICLE' | 'TRIP' = 'VEHICLE', code = 'OFFLINE_SYNC_BACKEND_UNAVAILABLE'): OfflineOperation {
  const aggregateId = aggregateType === 'VEHICLE' ? 'vehicle-1' : 'trip-1';
  return {
    operationId: 'operation-1', operationVersion: 1,
    operationType: 'VEHICLE_READING_RECORD', aggregateType: 'VEHICLE', aggregateId,
    payload: { readingType: 'ODOMETER', value: 100, recordedAt: '2026-08-22T10:00:00.000Z' },
    clientCreatedAt: '2026-08-22T10:00:00.000Z', clientUpdatedAt: '2026-08-22T10:00:00.000Z',
    clientInstanceId: 'client', idempotencyKey: 'operation-1', baseVersion: null,
    ownerUserId: 'owner', status: 'FAILED', attemptCount: 5, lastErrorCode: code,
    createdAt: '2026-08-22T10:00:00.000Z', updatedAt: '2026-08-22T10:00:00.000Z',
  } as OfflineOperation;
}

function renderActions(item = operation()) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidate = vi.spyOn(client, 'invalidateQueries');
  render(<ConfigProvider><App><QueryClientProvider client={client}><MemoryRouter>
    <OfflineOperationActions operation={item} />
  </MemoryRouter></QueryClientProvider></App></ConfigProvider>);
  return { invalidate };
}

describe('OfflineOperationActions', () => {
  beforeEach(() => vi.clearAllMocks());

  it('retries through the feature boundary and preserves the operation reference', async () => {
    const item = operation();
    const user = userEvent.setup();
    renderActions(item);
    await user.click(screen.getByRole('button', { name: /retry/i }));
    expect(retryOperation).toHaveBeenCalledWith(item.operationId);
  });

  it('requires confirmation before discarding the unsynchronized local copy', async () => {
    const item = operation();
    const user = userEvent.setup();
    renderActions(item);
    await user.click(screen.getByRole('button', { name: /discard/i }));
    expect(await screen.findByText(/server data is unchanged/i)).toBeInTheDocument();
    expect(discardOperation).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: /discard local copy/i }));
    expect(discardOperation).toHaveBeenCalledWith(item.operationId);
  });

  it('invalidates the complete owning vehicle query set on refresh', async () => {
    const user = userEvent.setup();
    const { invalidate } = renderActions(operation());
    await user.click(screen.getByRole('button', { name: /refresh/i }));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['vehicle-readings', 'vehicle-1'] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['vehicle-readings-latest', 'vehicle-1'] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['vehicles-page', 'vehicle-1'] });
  });

  it('opens the owning vehicle route and suppresses retry for non-retryable failures', async () => {
    const user = userEvent.setup();
    renderActions(operation('VEHICLE', 'PAYLOAD_INVALID'));
    expect(screen.queryByRole('button', { name: /retry/i })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /open/i }));
    expect(window.location.pathname + window.location.search).toBe('/fleet/vehicles?vehicleId=vehicle-1');
  });
});
