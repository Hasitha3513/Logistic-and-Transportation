import {
  OFFLINE_SYNC_BATCH_SIZE,
  OFFLINE_SYNC_CLAIM_LEASE_MILLISECONDS,
  OFFLINE_SYNC_MAX_AUTOMATIC_ATTEMPTS,
  OFFLINE_SYNC_SYNCED_RETENTION_DAYS,
} from './constants';
import { browserOfflineSyncConnectivity, type OfflineSyncConnectivity } from './connectivity';
import { OfflineSyncPostApplyRegistry, type OfflineSyncPostApplyCallback } from './postApplyRegistry';
import { systemOfflineSyncClock, type OfflineSyncClock } from './queue';
import { nextOfflineSyncRetry } from './retryPolicy';
import {
  OfflineSyncClientError,
  offlineSyncClient,
  toServerOperation,
  type OfflineSyncClient,
} from './syncClient';
import { IndexedDbOfflineOperationStorage } from './storage';
import type { OfflineOperation, OfflineOperationStorage, OfflineSyncOperationResult } from './types';

export interface OfflineSyncCoordinatorState {
  onlineHint: boolean;
  backendReachable: boolean | undefined;
  syncing: boolean;
  authPaused: boolean;
}

export interface OfflineSyncScheduler {
  set(callback: () => void, delay: number): number;
  clear(handle: number): void;
}

export interface OfflineSyncCoordinatorOptions {
  storage?: OfflineOperationStorage;
  client?: OfflineSyncClient;
  connectivity?: OfflineSyncConnectivity;
  clock?: OfflineSyncClock;
  scheduler?: OfflineSyncScheduler;
  postApplyRegistry?: OfflineSyncPostApplyRegistry;
}

const browserScheduler: OfflineSyncScheduler = {
  set: (callback, delay) => window.setTimeout(callback, delay),
  clear: (handle) => window.clearTimeout(handle),
};

function plusMilliseconds(timestamp: string, milliseconds: number): string {
  return new Date(Date.parse(timestamp) + milliseconds).toISOString();
}

function minusDays(timestamp: string, days: number): string {
  return new Date(Date.parse(timestamp) - days * 24 * 60 * 60 * 1_000).toISOString();
}

function safeText(value: string | null | undefined, fallback: string): string {
  const normalized = value?.replace(/\s+/g, ' ').trim();
  return (normalized || fallback).slice(0, 500);
}

export class OfflineSyncCoordinator {
  private readonly storage: OfflineOperationStorage;
  private readonly client: OfflineSyncClient;
  private readonly connectivity: OfflineSyncConnectivity;
  private readonly clock: OfflineSyncClock;
  private readonly scheduler: OfflineSyncScheduler;
  private readonly registry: OfflineSyncPostApplyRegistry;
  private readonly listeners = new Set<(state: OfflineSyncCoordinatorState) => void>();
  private state: OfflineSyncCoordinatorState;
  private ownerUserId?: string;
  private generation = 0;
  private inFlight?: Promise<void>;
  private timer?: number;
  private unsubscribeConnectivity?: () => void;

  constructor(options: OfflineSyncCoordinatorOptions = {}) {
    this.storage = options.storage ?? new IndexedDbOfflineOperationStorage();
    this.client = options.client ?? offlineSyncClient;
    this.connectivity = options.connectivity ?? browserOfflineSyncConnectivity;
    this.clock = options.clock ?? systemOfflineSyncClock;
    this.scheduler = options.scheduler ?? browserScheduler;
    this.registry = options.postApplyRegistry ?? new OfflineSyncPostApplyRegistry();
    this.state = {
      onlineHint: this.connectivity.isOnline(),
      backendReachable: undefined,
      syncing: false,
      authPaused: false,
    };
  }

  getState(): OfflineSyncCoordinatorState {
    return this.state;
  }

  subscribe(listener: (state: OfflineSyncCoordinatorState) => void): () => void {
    this.listeners.add(listener);
    listener(this.state);
    return () => this.listeners.delete(listener);
  }

  registerPostApply(
    operationType: OfflineOperation['operationType'],
    callback: OfflineSyncPostApplyCallback,
  ): () => void {
    return this.registry.register(operationType, callback);
  }

  async activate(ownerUserId: string): Promise<void> {
    this.deactivate();
    const generation = this.generation;
    this.ownerUserId = ownerUserId;
    this.updateState({
      onlineHint: this.connectivity.isOnline(),
      backendReachable: undefined,
      authPaused: false,
    });
    this.unsubscribeConnectivity = this.connectivity.subscribe((online) => {
      this.updateState({ onlineHint: online, backendReachable: online ? undefined : false });
      if (online) void this.syncNow();
      else this.clearTimer();
    });
    await this.storage.initialize();
    const now = this.clock.now();
    await this.storage.recoverExpiredClaims(ownerUserId, now);
    await this.storage.purgeSynced(ownerUserId, minusDays(now, OFFLINE_SYNC_SYNCED_RETENTION_DAYS));
    if (!this.isCurrent(ownerUserId, generation)) return;
    if (this.inFlight) await this.inFlight;
    if (this.isCurrent(ownerUserId, generation)) await this.syncNow();
  }

  deactivate(): void {
    this.generation += 1;
    this.ownerUserId = undefined;
    this.unsubscribeConnectivity?.();
    this.unsubscribeConnectivity = undefined;
    this.clearTimer();
    this.updateState({ syncing: false, authPaused: false, backendReachable: undefined });
  }

  dispose(): void {
    this.deactivate();
  }

  syncNow(): Promise<void> {
    if (this.inFlight) return this.inFlight;
    const ownerUserId = this.ownerUserId;
    const generation = this.generation;
    if (!ownerUserId || !this.state.onlineHint || this.state.authPaused) {
      return Promise.resolve();
    }
    this.clearTimer();
    const task = this.run(ownerUserId, generation);
    const run = task.finally(async () => {
      this.inFlight = undefined;
      if (this.isCurrent(ownerUserId, generation)) {
        this.updateState({ syncing: false });
        await this.scheduleNext(ownerUserId, generation);
      }
    });
    this.inFlight = run;
    return run;
  }

  private async run(ownerUserId: string, generation: number): Promise<void> {
    this.updateState({ syncing: true });
    await this.storage.recoverExpiredClaims(ownerUserId, this.clock.now());
    for (let batch = 0; batch < 20 && this.isCurrent(ownerUserId, generation); batch += 1) {
      if (!this.state.onlineHint || this.state.authPaused) break;
      const now = this.clock.now();
      const pending = await this.storage.getPending(ownerUserId, now, OFFLINE_SYNC_BATCH_SIZE);
      if (pending.length === 0) break;
      const claimed = await this.storage.claimForSync(
        ownerUserId,
        pending.map((operation) => operation.operationId),
        plusMilliseconds(now, OFFLINE_SYNC_CLAIM_LEASE_MILLISECONDS),
      );
      if (claimed.length === 0) break;
      await this.sendBatch(ownerUserId, generation, claimed, now);
      if (this.state.authPaused || !this.state.onlineHint || this.state.backendReachable === false) break;
    }
  }

  private async sendBatch(
    ownerUserId: string,
    generation: number,
    claimed: OfflineOperation[],
    lastAttemptAt: string,
  ): Promise<void> {
    try {
      const response = await this.client.synchronize(claimed.map(toServerOperation));
      if (!this.isCurrent(ownerUserId, generation)) {
        await this.releaseClaims(ownerUserId, claimed);
        return;
      }
      this.updateState({ backendReachable: true });
      const claimedIds = new Set(claimed.map((operation) => operation.operationId));
      const results = new Map<string, OfflineSyncOperationResult>();
      for (const result of response.results) {
        if (!claimedIds.has(result.operationId) || results.has(result.operationId)) {
          await this.retryAll(ownerUserId, claimed, lastAttemptAt, 'OFFLINE_SYNC_PROTOCOL_ERROR', 'Invalid operation results');
          return;
        }
        results.set(result.operationId, result);
      }
      for (const operation of claimed) {
        const result = results.get(operation.operationId);
        if (!result) {
          await this.retry(operation, ownerUserId, lastAttemptAt, 'OFFLINE_SYNC_PROTOCOL_ERROR', 'Operation result is missing');
        } else {
          await this.applyResult(ownerUserId, operation, result, lastAttemptAt);
        }
      }
    } catch (error: unknown) {
      if (!this.isCurrent(ownerUserId, generation)) {
        await this.releaseClaims(ownerUserId, claimed);
        return;
      }
      if (error instanceof OfflineSyncClientError && error.kind === 'HTTP') {
        this.updateState({ backendReachable: true });
        if (error.httpStatus === 401) {
          await this.releaseClaims(ownerUserId, claimed);
          this.updateState({ authPaused: true });
          return;
        }
        if (error.httpStatus === 403) {
          await this.failAll(ownerUserId, claimed, lastAttemptAt, 'OFFLINE_SYNC_FORBIDDEN', 'Offline synchronization is forbidden');
          return;
        }
        if (error.httpStatus === 400 || (error.httpStatus !== undefined && error.httpStatus >= 400 && error.httpStatus < 500)) {
          await this.failAll(ownerUserId, claimed, lastAttemptAt, 'OFFLINE_SYNC_PROTOCOL_ERROR', 'Offline synchronization request was rejected');
          return;
        }
      }
      if (error instanceof OfflineSyncClientError && error.kind === 'PROTOCOL') {
        this.updateState({ backendReachable: true });
        await this.retryAll(ownerUserId, claimed, lastAttemptAt, error.code, error.message);
        return;
      }
      this.updateState({ backendReachable: false });
      await this.retryAll(ownerUserId, claimed, lastAttemptAt, 'OFFLINE_SYNC_BACKEND_UNAVAILABLE', 'Offline synchronization backend is unavailable');
    }
  }

  private async applyResult(
    ownerUserId: string,
    operation: OfflineOperation,
    result: OfflineSyncOperationResult,
    lastAttemptAt: string,
  ): Promise<void> {
    const attemptCount = operation.attemptCount + 1;
    const common = {
      serverProcessedAt: result.serverTimestamp,
      serverAggregateId: result.aggregateId,
      serverResultStatus: result.status,
      attemptCount,
      lastAttemptAt,
    } as const;
    if (result.status === 'APPLIED' || result.status === 'ALREADY_APPLIED') {
      const synced = await this.storage.markSynced(ownerUserId, operation.operationId, common);
      await this.registry.notify(synced, result).catch(() => undefined);
    } else if (result.status === 'REJECTED') {
      await this.storage.markFailed(ownerUserId, operation.operationId, {
        ...common,
        errorCode: safeText(result.errorCode, 'OFFLINE_SYNC_REJECTED'),
        errorMessage: safeText(result.message, 'Offline operation was rejected'),
      });
    } else if (result.status === 'CONFLICT') {
      await this.storage.markConflict(ownerUserId, operation.operationId, {
        ...common,
        errorCode: safeText(result.errorCode, 'OFFLINE_SYNC_CONFLICT'),
        errorMessage: safeText(result.message, 'Offline operation conflicts with current data'),
      });
    } else {
      await this.retry(
        operation,
        ownerUserId,
        lastAttemptAt,
        safeText(result.errorCode, 'OFFLINE_SYNC_RETRYABLE_ERROR'),
        safeText(result.message, 'Offline operation can be retried'),
      );
    }
  }

  private async retryAll(
    ownerUserId: string,
    operations: OfflineOperation[],
    lastAttemptAt: string,
    errorCode: string,
    errorMessage: string,
  ): Promise<void> {
    await Promise.all(operations.map((operation) => this.retry(operation, ownerUserId, lastAttemptAt, errorCode, errorMessage)));
  }

  private async retry(
    operation: OfflineOperation,
    ownerUserId: string,
    lastAttemptAt: string,
    errorCode: string,
    errorMessage: string,
  ): Promise<void> {
    const attemptCount = operation.attemptCount + 1;
    const nextAttemptAt = nextOfflineSyncRetry(attemptCount, lastAttemptAt);
    const error = { errorCode: safeText(errorCode, 'OFFLINE_SYNC_RETRYABLE_ERROR'), errorMessage: safeText(errorMessage, 'Retry required') };
    if (!nextAttemptAt || attemptCount >= OFFLINE_SYNC_MAX_AUTOMATIC_ATTEMPTS) {
      await this.storage.markFailed(ownerUserId, operation.operationId, { ...error, attemptCount, lastAttemptAt });
    } else {
      await this.storage.releaseForRetry(ownerUserId, operation.operationId, { ...error, attemptCount, lastAttemptAt, nextAttemptAt });
    }
  }

  private async failAll(
    ownerUserId: string,
    operations: OfflineOperation[],
    lastAttemptAt: string,
    errorCode: string,
    errorMessage: string,
  ): Promise<void> {
    await Promise.all(operations.map((operation) => this.storage.markFailed(ownerUserId, operation.operationId, {
      errorCode,
      errorMessage,
      attemptCount: operation.attemptCount + 1,
      lastAttemptAt,
    })));
  }

  private async releaseClaims(ownerUserId: string, operations: OfflineOperation[]): Promise<void> {
    await Promise.all(operations.map((operation) => this.storage.releaseClaim(ownerUserId, operation.operationId).catch(() => undefined)));
  }

  private async scheduleNext(ownerUserId: string, generation: number): Promise<void> {
    this.clearTimer();
    if (!this.isCurrent(ownerUserId, generation) || !this.state.onlineHint || this.state.authPaused) return;
    const nextAttemptAt = await this.storage.getNextPendingAt(ownerUserId);
    if (!nextAttemptAt || !this.isCurrent(ownerUserId, generation)) return;
    const delay = Math.max(0, Date.parse(nextAttemptAt) - Date.parse(this.clock.now()));
    this.timer = this.scheduler.set(() => {
      this.timer = undefined;
      void this.syncNow();
    }, Math.min(delay, 2_147_483_647));
  }

  private clearTimer(): void {
    if (this.timer !== undefined) this.scheduler.clear(this.timer);
    this.timer = undefined;
  }

  private isCurrent(ownerUserId: string, generation: number): boolean {
    return this.ownerUserId === ownerUserId && this.generation === generation;
  }

  private updateState(update: Partial<OfflineSyncCoordinatorState>): void {
    this.state = { ...this.state, ...update };
    this.listeners.forEach((listener) => listener(this.state));
  }
}
