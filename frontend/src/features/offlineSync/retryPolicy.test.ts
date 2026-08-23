import { describe, expect, it } from 'vitest';
import { nextOfflineSyncRetry, offlineSyncRetryDelay } from './retryPolicy';

describe('offline sync retry policy', () => {
  it('uses the frozen retry sequence and stops after ten attempts', () => {
    expect([1, 2, 3, 4, 9].map(offlineSyncRetryDelay)).toEqual([5_000, 15_000, 30_000, 60_000, 60_000]);
    expect(nextOfflineSyncRetry(1, '2026-08-22T10:00:00.000Z')).toBe('2026-08-22T10:00:05.000Z');
    expect(nextOfflineSyncRetry(2, '2026-08-22T10:00:00.000Z')).toBe('2026-08-22T10:00:15.000Z');
    expect(nextOfflineSyncRetry(3, '2026-08-22T10:00:00.000Z')).toBe('2026-08-22T10:00:30.000Z');
    expect(nextOfflineSyncRetry(4, '2026-08-22T10:00:00.000Z')).toBe('2026-08-22T10:01:00.000Z');
    expect(nextOfflineSyncRetry(10, '2026-08-22T10:00:00.000Z')).toBeUndefined();
  });
});
