import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App, ConfigProvider } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { CurrentUser } from '../../auth/types';
import type { OfflineOperation } from './types';
import { OfflineSyncCenter } from './OfflineSyncCenter';

const syncNow = vi.fn(() => Promise.resolve());
const retryOperation = vi.fn(() => Promise.resolve({} as OfflineOperation));
const discardOperation = vi.fn(() => Promise.resolve(true));
let currentUser: CurrentUser | undefined;
let operations: OfflineOperation[] = [];
let syncState = {
  onlineHint: true, backendReachable: true as boolean | undefined, syncing: false, authPaused: false,
};

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({ user: currentUser }),
}));
vi.mock('./OfflineSyncProvider', () => ({
  useOptionalOfflineSync: () => ({
    ...syncState, operationsRevision: 0, syncNow,
    getOperations: () => Promise.resolve(operations), retryOperation, discardOperation,
  }),
  useOfflineSync: () => ({ retryOperation, discardOperation }),
}));

function operation(status: OfflineOperation['status'], code?: string): OfflineOperation {
  return {
    operationId: `${status}-id`, operationVersion: 1, operationType: 'VEHICLE_READING_RECORD',
    aggregateType: 'VEHICLE', aggregateId: 'vehicle-1',
    payload: { readingType: 'ODOMETER', value: 123, recordedAt: '2026-08-22T10:00:00.000Z' },
    clientCreatedAt: '2026-08-22T10:00:00.000Z', clientUpdatedAt: '2026-08-22T10:00:00.000Z',
    clientInstanceId: 'client', idempotencyKey: `${status}-id`, baseVersion: null,
    ownerUserId: currentUser?.id ?? 'owner-a', status, attemptCount: 1, lastErrorCode: code,
    lastErrorMessage: code ? 'Safe operational message' : undefined,
    createdAt: '2026-08-22T10:00:00.000Z', updatedAt: '2026-08-22T10:00:00.000Z',
  };
}

function renderCenter() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<ConfigProvider><App><QueryClientProvider client={client}><MemoryRouter>
    <OfflineSyncCenter />
  </MemoryRouter></QueryClientProvider></App></ConfigProvider>);
}

describe('OfflineSyncCenter', () => {
  beforeEach(() => {
    currentUser = { id: 'owner-a', username: 'owner.a', email: 'a@example.com', firstName: 'A', lastName: 'Owner', active: true, roles: [], permissions: [] };
    operations = [];
    syncState = { onlineHint: true, backendReachable: true, syncing: false, authPaused: false };
    vi.clearAllMocks();
  });

  it('shows owner-scoped status counts and actionable safe details', async () => {
    operations = [operation('PENDING'), operation('SYNCING'), operation('CONFLICT', 'OFFLINE_SYNC_CONFLICT'), operation('FAILED', 'OFFLINE_SYNC_BACKEND_UNAVAILABLE')];
    const user = userEvent.setup();
    renderCenter();

    await user.click(await screen.findByRole('button', { name: /offline synchronization status: online/i }));
    expect(await screen.findByText('Offline synchronization')).toBeInTheDocument();
    expect(screen.getAllByText('Safe operational message')).toHaveLength(2);
    expect(screen.getAllByText('Conflict').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Failed').length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: /sync now/i })).toBeEnabled();
  });

  it('uses the coordinator for manual synchronization', async () => {
    operations = [operation('PENDING')];
    const user = userEvent.setup();
    renderCenter();
    await user.click(await screen.findByRole('button', { name: /offline synchronization status: online/i }));
    await user.click(screen.getByRole('button', { name: /sync now/i }));
    expect(syncNow).toHaveBeenCalledOnce();
  });

  it('shows authentication pause and disables sync without deleting or failing local work', async () => {
    syncState = { onlineHint: true, backendReachable: true, syncing: false, authPaused: true };
    operations = [operation('PENDING')];
    const user = userEvent.setup();
    renderCenter();
    await user.click(await screen.findByRole('button', { name: /authentication paused/i }));
    expect(await screen.findByText(/paused until your authenticated session is restored/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sync now/i })).toBeDisabled();
    expect(discardOperation).not.toHaveBeenCalled();
  });

  it('refreshes the visible list when the authenticated owner changes', async () => {
    operations = [operation('FAILED', 'OFFLINE_SYNC_BACKEND_UNAVAILABLE')];
    const view = renderCenter();
    expect(await screen.findByLabelText(/offline synchronization status/i)).toBeInTheDocument();
    currentUser = { ...currentUser!, id: 'owner-b', username: 'owner.b' };
    operations = [];
    view.rerender(<ConfigProvider><App><QueryClientProvider client={new QueryClient()}><MemoryRouter>
      <OfflineSyncCenter />
    </MemoryRouter></QueryClientProvider></App></ConfigProvider>);
    await waitFor(() => expect(screen.getByRole('button', { name: /offline synchronization status: online/i })).toBeInTheDocument());
  });
});
