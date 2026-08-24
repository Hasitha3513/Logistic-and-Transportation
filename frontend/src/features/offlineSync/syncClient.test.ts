import { AxiosError } from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '../../api/client';
import { createPendingOfflineOperation, type OfflineSyncClock, type OfflineSyncUuidGenerator } from './queue';
import { AxiosOfflineSyncClient, OfflineSyncClientError, toServerOperation } from './syncClient';

const clock: OfflineSyncClock = { now: () => '2026-08-22T10:00:00.000Z' };
const uuid: OfflineSyncUuidGenerator = { randomUUID: () => '30000000-0000-4000-8000-000000000001' };

function operation() {
  return createPendingOfflineOperation({
    ownerUserId: '10000000-0000-4000-8000-000000000001',
    operationType: 'VEHICLE_READING_RECORD',
    aggregateType: 'VEHICLE',
    aggregateId: '20000000-0000-4000-8000-000000000001',
    payload: { readingType: 'ODOMETER', value: 12, recordedAt: clock.now() },
  }, '40000000-0000-4000-8000-000000000001', clock, uuid);
}

describe('AxiosOfflineSyncClient', () => {
  afterEach(() => vi.restoreAllMocks());

  it('maps only the frozen server-bound fields and parses a valid response', async () => {
    const item = operation();
    const serverItem = toServerOperation(item);
    expect(Object.keys(serverItem)).toEqual([
      'operationId', 'operationVersion', 'operationType', 'aggregateType', 'aggregateId', 'payload',
      'clientCreatedAt', 'clientUpdatedAt', 'clientInstanceId', 'idempotencyKey', 'baseVersion',
    ]);
    expect(serverItem).not.toHaveProperty('ownerUserId');
    expect(serverItem).not.toHaveProperty('status');

    const result = {
      operationId: item.operationId,
      status: 'APPLIED' as const,
      serverTimestamp: clock.now(),
      aggregateId: item.aggregateId,
      currentVersion: null,
      errorCode: null,
      message: null,
    };
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: { serverTimestamp: clock.now(), results: [result] } });
    await expect(new AxiosOfflineSyncClient().synchronize([serverItem])).resolves.toEqual({ serverTimestamp: clock.now(), results: [result] });
    expect(post).toHaveBeenCalledWith('/offline-sync/operations', { operations: [serverItem] });
  });

  it('rejects unknown result statuses as retryable protocol failures', async () => {
    const item = toServerOperation(operation());
    const post = vi.spyOn(api, 'post').mockResolvedValue({
      data: { serverTimestamp: clock.now(), results: [{ operationId: item.operationId, status: 'NEW_STATUS' }] },
    });
    await expect(new AxiosOfflineSyncClient().synchronize([item])).rejects.toMatchObject({
      kind: 'PROTOCOL', code: 'OFFLINE_SYNC_PROTOCOL_ERROR',
    });
    post.mockResolvedValueOnce({
      data: {
        serverTimestamp: 'not-a-timestamp',
        results: [],
      },
    });
    await expect(new AxiosOfflineSyncClient().synchronize([item])).rejects.toMatchObject({
      kind: 'PROTOCOL', code: 'OFFLINE_SYNC_PROTOCOL_ERROR',
    });
  });

  it('classifies final HTTP and network errors without retaining raw transport details', async () => {
    const item = toServerOperation(operation());
    vi.spyOn(api, 'post').mockRejectedValueOnce(new AxiosError('secret', 'ERR_BAD_RESPONSE', undefined, undefined, { status: 401 } as never));
    await expect(new AxiosOfflineSyncClient().synchronize([item])).rejects.toEqual(
      expect.objectContaining<Partial<OfflineSyncClientError>>({ kind: 'HTTP', httpStatus: 401 }),
    );
    vi.spyOn(api, 'post').mockRejectedValueOnce(new AxiosError('private network detail'));
    await expect(new AxiosOfflineSyncClient().synchronize([item])).rejects.toEqual(
      expect.objectContaining<Partial<OfflineSyncClientError>>({ kind: 'NETWORK', code: 'OFFLINE_SYNC_NETWORK_ERROR' }),
    );
  });
});
