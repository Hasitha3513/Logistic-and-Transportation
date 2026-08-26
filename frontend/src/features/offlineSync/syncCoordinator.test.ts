import { IDBFactory } from 'fake-indexeddb';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { OfflineSyncConnectivity } from './connectivity';
import { OfflineSyncPostApplyRegistry } from './postApplyRegistry';
import type { OfflineSyncClock, OfflineSyncUuidGenerator } from './queue';
import { OfflineSyncClientError, type OfflineSyncClient } from './syncClient';
import { OfflineSyncCoordinator, type OfflineSyncScheduler } from './syncCoordinator';
import { IndexedDbOfflineOperationStorage } from './storage';
import type { OfflineOperation, OfflineOperationInput, OfflineServerOperation, OfflineSyncBatchResponse, OfflineSyncOperationResult } from './types';

const OWNER_A = '10000000-0000-4000-8000-000000000001';
const OWNER_B = '10000000-0000-4000-8000-000000000002';
const VEHICLE_ID = '20000000-0000-4000-8000-000000000001';

class MutableClock implements OfflineSyncClock {
  constructor(private value = '2026-08-22T10:00:00.000Z') {}
  now(): string { return this.value; }
  set(value: string): void { this.value = value; }
}

class SequentialUuid implements OfflineSyncUuidGenerator {
  private next = 1;
  randomUUID(): string { return `00000000-0000-4000-8000-${(this.next++).toString(16).padStart(12, '0')}`; }
}

class Connectivity implements OfflineSyncConnectivity {
  private listeners = new Set<(online: boolean) => void>();
  constructor(private online = true) {}
  isOnline(): boolean { return this.online; }
  subscribe(listener: (online: boolean) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }
  change(online: boolean): void {
    this.online = online;
    this.listeners.forEach((listener) => listener(online));
  }
}

class StubClient implements OfflineSyncClient {
  readonly calls: OfflineServerOperation[][] = [];
  responses: Array<OfflineSyncBatchResponse | Error | Promise<OfflineSyncBatchResponse>> = [];
  async synchronize(operations: readonly OfflineServerOperation[]): Promise<OfflineSyncBatchResponse> {
    this.calls.push([...operations]);
    const response = this.responses.shift();
    if (response instanceof Error) throw response;
    if (!response) return batch(operations.map((operation) => result(operation, 'APPLIED')));
    return response;
  }
}

class RecordingScheduler implements OfflineSyncScheduler {
  callback?: () => void;
  delay?: number;
  set(callback: () => void, delay: number): number { this.callback = callback; this.delay = delay; return 1; }
  clear(): void { this.callback = undefined; this.delay = undefined; }
  fire(): void { const callback = this.callback; this.callback = undefined; callback?.(); }
}

function input(ownerUserId = OWNER_A, value = 100): OfflineOperationInput {
  return {
    ownerUserId,
    operationType: 'VEHICLE_READING_RECORD',
    aggregateType: 'VEHICLE',
    aggregateId: VEHICLE_ID,
    payload: { readingType: 'ODOMETER', value, recordedAt: '2026-08-22T10:00:00.000Z' },
  };
}

function result(operation: Pick<OfflineOperation | OfflineServerOperation, 'operationId' | 'aggregateId'>, status: OfflineSyncOperationResult['status']): OfflineSyncOperationResult {
  return {
    operationId: operation.operationId,
    status,
    serverTimestamp: '2026-08-22T10:00:01.000Z',
    aggregateId: operation.aggregateId,
    currentVersion: null,
    errorCode: status === 'APPLIED' || status === 'ALREADY_APPLIED' ? null : `SERVER_${status}`,
    message: status === 'APPLIED' || status === 'ALREADY_APPLIED' ? null : `Server ${status}`,
  };
}

function batch(results: OfflineSyncOperationResult[]): OfflineSyncBatchResponse {
  return { serverTimestamp: '2026-08-22T10:00:01.000Z', results };
}

describe('OfflineSyncCoordinator', () => {
  let indexedDb: IDBFactory;
  let clock: MutableClock;
  let uuid: SequentialUuid;
  let storage: IndexedDbOfflineOperationStorage;
  let client: StubClient;
  let connectivity: Connectivity;
  let scheduler: RecordingScheduler;

  beforeEach(() => {
    indexedDb = new IDBFactory();
    clock = new MutableClock();
    uuid = new SequentialUuid();
    storage = new IndexedDbOfflineOperationStorage({ indexedDb, databaseName: `coordinator-${crypto.randomUUID()}`, clock, uuidGenerator: uuid });
    client = new StubClient();
    connectivity = new Connectivity();
    scheduler = new RecordingScheduler();
  });

  function coordinator(registry?: OfflineSyncPostApplyRegistry, adapter = storage, requestClient = client) {
    return new OfflineSyncCoordinator({ storage: adapter, client: requestClient, connectivity, clock, scheduler, postApplyRegistry: registry });
  }

  it('maps APPLIED and ALREADY_APPLIED to SYNCED and invokes post-apply extension callbacks', async () => {
    const first = await storage.enqueue(input());
    const second = await storage.enqueue(input(OWNER_A, 101));
    client.responses.push(batch([result(first, 'APPLIED'), result(second, 'ALREADY_APPLIED')]));
    const registry = new OfflineSyncPostApplyRegistry();
    const callback = vi.fn();
    registry.register('VEHICLE_READING_RECORD', callback);
    await coordinator(registry).activate(OWNER_A);

    await expect(storage.getById(OWNER_A, first.operationId)).resolves.toMatchObject({ status: 'SYNCED', attemptCount: 1, serverResultStatus: 'APPLIED' });
    await expect(storage.getById(OWNER_A, second.operationId)).resolves.toMatchObject({ status: 'SYNCED', attemptCount: 1, serverResultStatus: 'ALREADY_APPLIED' });
    expect(callback).toHaveBeenCalledTimes(2);
    expect(client.calls[0][0]).not.toHaveProperty('ownerUserId');
  });

  it('maps mixed per-item terminal and retryable results independently', async () => {
    const operations = await Promise.all([1, 2, 3, 4].map((value) => storage.enqueue(input(OWNER_A, value))));
    client.responses.push(batch([
      result(operations[0], 'APPLIED'), result(operations[1], 'REJECTED'),
      result(operations[2], 'CONFLICT'), result(operations[3], 'RETRYABLE_ERROR'),
    ]));
    await coordinator().activate(OWNER_A);
    const saved = await Promise.all(operations.map((operation) => storage.getById(OWNER_A, operation.operationId)));
    expect(saved.map((operation) => operation?.status)).toEqual(['SYNCED', 'FAILED', 'CONFLICT', 'PENDING']);
    expect(saved[3]).toMatchObject({ attemptCount: 1, nextAttemptAt: '2026-08-22T10:00:05.000Z' });
  });

  it('uses navigator connectivity only as a hint and synchronizes after an online event', async () => {
    connectivity = new Connectivity(false);
    const operation = await storage.enqueue(input());
    const active = coordinator();
    await active.activate(OWNER_A);
    expect(client.calls).toHaveLength(0);
    connectivity.change(true);
    await vi.waitFor(() => expect(client.calls).toHaveLength(1));
    await expect(storage.getById(OWNER_A, operation.operationId)).resolves.toMatchObject({ status: 'SYNCED' });
  });

  it('retries network failures with sanitized metadata and exact scheduling', async () => {
    const operation = await storage.enqueue(input());
    client.responses.push(new OfflineSyncClientError('NETWORK', 'RAW_SECRET', 'token=secret'));
    await coordinator().activate(OWNER_A);
    await expect(storage.getById(OWNER_A, operation.operationId)).resolves.toMatchObject({
      status: 'PENDING', attemptCount: 1, nextAttemptAt: '2026-08-22T10:00:05.000Z',
      lastErrorCode: 'OFFLINE_SYNC_BACKEND_UNAVAILABLE', lastErrorMessage: 'Offline synchronization backend is unavailable',
    });
  });

  it('pauses on final 401 without incrementing attempts and resumes for the same authenticated user', async () => {
    const operation = await storage.enqueue(input());
    client.responses.push(new OfflineSyncClientError('HTTP', 'HTTP', 'unauthorized', 401));
    const active = coordinator();
    await active.activate(OWNER_A);
    expect(active.getState().authPaused).toBe(true);
    await expect(storage.getById(OWNER_A, operation.operationId)).resolves.toMatchObject({ status: 'PENDING', attemptCount: 0 });
    await active.syncNow();
    expect(client.calls).toHaveLength(1);
    await active.activate(OWNER_A);
    await expect(storage.getById(OWNER_A, operation.operationId)).resolves.toMatchObject({ status: 'SYNCED', attemptCount: 1 });
  });

  it('isolates pending work by user across an authentication change', async () => {
    const operation = await storage.enqueue(input(OWNER_A));
    const active = coordinator();
    connectivity = new Connectivity(false);
    const isolated = coordinator();
    await isolated.activate(OWNER_A);
    await isolated.activate(OWNER_B);
    connectivity.change(true);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(client.calls).toHaveLength(0);
    await expect(storage.getById(OWNER_A, operation.operationId)).resolves.toMatchObject({ status: 'PENDING' });
    active.dispose();
  });

  it('deduplicates concurrent same-tab triggers', async () => {
    await storage.enqueue(input());
    let resolve!: (response: OfflineSyncBatchResponse) => void;
    client.responses.push(new Promise((done) => { resolve = done; }));
    connectivity = new Connectivity(false);
    const active = coordinator();
    await active.activate(OWNER_A);
    connectivity.change(true);
    const first = active.syncNow();
    const second = active.syncNow();
    await vi.waitFor(() => expect(client.calls).toHaveLength(1));
    resolve(batch(client.calls[0].map((operation) => result(operation, 'APPLIED'))));
    await Promise.all([first, second]);
    expect(client.calls).toHaveLength(1);
  });

  it('uses the real IndexedDB lease to prevent duplicate cross-tab sends', async () => {
    await storage.enqueue(input());
    const secondStorage = new IndexedDbOfflineOperationStorage({
      indexedDb, databaseName: (storage as unknown as { databaseName: string }).databaseName, clock, uuidGenerator: uuid,
    });
    let resolve!: (response: OfflineSyncBatchResponse) => void;
    client.responses.push(new Promise((done) => { resolve = done; }));
    connectivity = new Connectivity(false);
    const first = coordinator(undefined, storage);
    const second = coordinator(undefined, secondStorage);
    await Promise.all([first.activate(OWNER_A), second.activate(OWNER_A)]);
    connectivity.change(true);
    await vi.waitFor(() => expect(client.calls).toHaveLength(1));
    resolve(batch(client.calls[0].map((operation) => result(operation, 'APPLIED'))));
    await vi.waitFor(async () => expect((await storage.countByStatus(OWNER_A)).SYNCED).toBe(1));
  });

  it('retries a missing result and a protocol/unknown-status response', async () => {
    const first = await storage.enqueue(input());
    const second = await storage.enqueue(input(OWNER_A, 2));
    client.responses.push(batch([result(first, 'APPLIED')]));
    await coordinator().activate(OWNER_A);
    await expect(storage.getById(OWNER_A, first.operationId)).resolves.toMatchObject({ status: 'SYNCED' });
    await expect(storage.getById(OWNER_A, second.operationId)).resolves.toMatchObject({ status: 'PENDING', lastErrorCode: 'OFFLINE_SYNC_PROTOCOL_ERROR' });

    clock.set('2026-08-22T10:00:05.000Z');
    client.responses.push(new OfflineSyncClientError('PROTOCOL', 'OFFLINE_SYNC_PROTOCOL_ERROR', 'unknown status'));
    await coordinator().activate(OWNER_A);
    await expect(storage.getById(OWNER_A, second.operationId)).resolves.toMatchObject({ attemptCount: 2, nextAttemptAt: '2026-08-22T10:00:20.000Z' });
  });

  it('handles top-level 400, 403 and 5xx according to policy', async () => {
    for (const [status, code, expected] of [[400, 'OFFLINE_SYNC_PROTOCOL_ERROR', 'FAILED'], [403, 'OFFLINE_SYNC_FORBIDDEN', 'FAILED'], [503, 'OFFLINE_SYNC_BACKEND_UNAVAILABLE', 'PENDING']] as const) {
      const localStorageAdapter = new IndexedDbOfflineOperationStorage({ indexedDb: new IDBFactory(), databaseName: `http-${status}`, clock, uuidGenerator: new SequentialUuid() });
      const operation = await localStorageAdapter.enqueue(input());
      const localClient = new StubClient();
      localClient.responses.push(new OfflineSyncClientError('HTTP', 'HTTP', 'raw', status));
      await coordinator(undefined, localStorageAdapter, localClient).activate(OWNER_A);
      await expect(localStorageAdapter.getById(OWNER_A, operation.operationId)).resolves.toMatchObject({ status: expected, lastErrorCode: code, attemptCount: 1 });
    }
  });

  it('stops automatic retry at attempt ten', async () => {
    const operation = await storage.enqueue(input());
    const claimed = await storage.claimForSync(OWNER_A, [operation.operationId], '2026-08-22T10:00:30.000Z');
    await storage.releaseForRetry(OWNER_A, operation.operationId, {
      attemptCount: 9, lastAttemptAt: clock.now(), nextAttemptAt: clock.now(), errorCode: 'OLD', errorMessage: 'old',
    });
    expect(claimed).toHaveLength(1);
    client.responses.push(new OfflineSyncClientError('NETWORK', 'NETWORK', 'network'));
    await coordinator().activate(OWNER_A);
    await expect(storage.getById(OWNER_A, operation.operationId)).resolves.toMatchObject({ status: 'FAILED', attemptCount: 10 });
  });

  it('processes subsequent batches without sending more than fifty operations', async () => {
    await Promise.all(Array.from({ length: 51 }, (_, index) => storage.enqueue(input(OWNER_A, index))));
    await coordinator().activate(OWNER_A);
    expect(client.calls.map((call) => call.length)).toEqual([50, 1]);
  });

  it('schedules exactly the earliest due retry and clears it on logout', async () => {
    const operation = await storage.enqueue(input());
    const claimed = await storage.claimForSync(OWNER_A, [operation.operationId], '2026-08-22T10:00:30.000Z');
    await storage.releaseForRetry(OWNER_A, operation.operationId, {
      attemptCount: 1, lastAttemptAt: clock.now(), nextAttemptAt: '2026-08-22T10:00:05.000Z', errorCode: 'NETWORK', errorMessage: 'retry',
    });
    expect(claimed).toHaveLength(1);
    const active = coordinator();
    await active.activate(OWNER_A);
    expect(scheduler.delay).toBe(5_000);
    active.deactivate();
    expect(scheduler.callback).toBeUndefined();
    await expect(storage.getById(OWNER_A, operation.operationId)).resolves.toMatchObject({ status: 'PENDING' });
  });

  it('recovers expired claims, preserves live claims, and purges synced records older than seven days', async () => {
    const expired = await storage.enqueue(input());
    const live = await storage.enqueue(input(OWNER_A, 2));
    await storage.claimForSync(OWNER_A, [expired.operationId], '2026-08-22T09:59:59.000Z');
    await storage.claimForSync(OWNER_A, [live.operationId], '2026-08-22T10:00:30.000Z');
    connectivity = new Connectivity(false);
    await coordinator().activate(OWNER_A);
    await expect(storage.getById(OWNER_A, expired.operationId)).resolves.toMatchObject({ status: 'PENDING' });
    await expect(storage.getById(OWNER_A, live.operationId)).resolves.toMatchObject({ status: 'SYNCING' });

    clock.set('2026-08-22T10:00:31.000Z');
    connectivity.change(true);
    await vi.waitFor(async () => expect((await storage.countByStatus(OWNER_A)).SYNCED).toBe(2));
    clock.set('2026-08-30T10:00:32.000Z');
    connectivity = new Connectivity(false);
    await coordinator().activate(OWNER_A);
    expect(await storage.getById(OWNER_A, expired.operationId)).toBeUndefined();
    expect(await storage.getById(OWNER_A, live.operationId)).toBeUndefined();
  });
});
