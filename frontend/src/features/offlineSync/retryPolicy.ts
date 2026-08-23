import { OFFLINE_SYNC_MAX_AUTOMATIC_ATTEMPTS } from './constants';

export function offlineSyncRetryDelay(attemptCount: number): number {
  if (attemptCount <= 1) return 5_000;
  if (attemptCount === 2) return 15_000;
  if (attemptCount === 3) return 30_000;
  return 60_000;
}

export function nextOfflineSyncRetry(attemptCount: number, lastAttemptAt: string): string | undefined {
  if (attemptCount >= OFFLINE_SYNC_MAX_AUTOMATIC_ATTEMPTS) return undefined;
  return new Date(Date.parse(lastAttemptAt) + offlineSyncRetryDelay(attemptCount)).toISOString();
}
