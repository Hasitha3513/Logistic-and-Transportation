import { describe, expect, it } from 'vitest';
import { getOfflineOperationActions, OFFLINE_STATUS_PRESENTATION } from './presentation';
import type { OfflineOperation } from './types';

function failed(code: string, status: 'FAILED' | 'CONFLICT' = 'FAILED'): OfflineOperation {
  return {
    operationId: '40000000-0000-4000-8000-000000000001', operationVersion: 1,
    operationType: 'VEHICLE_READING_RECORD', aggregateType: 'VEHICLE', aggregateId: 'vehicle-1',
    payload: { readingType: 'ODOMETER', value: 100, recordedAt: '2026-08-22T10:00:00.000Z' },
    clientCreatedAt: '2026-08-22T10:00:00.000Z', clientUpdatedAt: '2026-08-22T10:00:00.000Z',
    clientInstanceId: 'client-1', idempotencyKey: '40000000-0000-4000-8000-000000000001', baseVersion: null,
    ownerUserId: 'owner-1', status, attemptCount: 5, lastErrorCode: code,
    createdAt: '2026-08-22T10:00:00.000Z', updatedAt: '2026-08-22T10:00:00.000Z',
  };
}

describe('offline synchronization presentation and action policy', () => {
  it('provides one friendly presentation for every frozen local status', () => {
    expect(Object.keys(OFFLINE_STATUS_PRESENTATION)).toEqual(['PENDING', 'SYNCING', 'SYNCED', 'FAILED', 'CONFLICT']);
    expect(OFFLINE_STATUS_PRESENTATION.PENDING.label).toBe('Pending');
  });

  it.each(['FORBIDDEN', 'CONFLICT', 'IDEMPOTENCY_MISMATCH', 'PAYLOAD_INVALID'])(
    'does not offer a blind retry for %s',
    (code) => expect(getOfflineOperationActions(failed(code))).toEqual({
      open: true, refresh: true, retry: false, discard: true,
    }),
  );

  it('allows retry for an exhausted transient failure but never for a conflict', () => {
    expect(getOfflineOperationActions(failed('OFFLINE_SYNC_BACKEND_UNAVAILABLE')).retry).toBe(true);
    expect(getOfflineOperationActions(failed('OFFLINE_SYNC_CONFLICT', 'CONFLICT')).retry).toBe(false);
  });

  it('does not expose terminal actions for pending, syncing, or synced operations', () => {
    for (const status of ['PENDING', 'SYNCING', 'SYNCED'] as const) {
      expect(getOfflineOperationActions({ ...failed(''), status })).toEqual({
        open: false, refresh: false, retry: false, discard: false,
      });
    }
  });
});
