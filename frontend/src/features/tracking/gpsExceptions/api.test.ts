import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../../../api/client';
import { gpsExceptionApi } from './api';

vi.mock('../../../api/client', () => ({ api: { get: vi.fn(), post: vi.fn() } }));

describe('gpsExceptionApi', () => {
  beforeEach(() => { vi.mocked(api.get).mockReset(); vi.mocked(api.post).mockReset(); });

  it('sends bounded filters and the opaque cursor as query parameters', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: { items: [], nextCursor: 'opaque' } });
    const filters = { from: '2026-09-16T00:00:00Z', to: '2026-09-17T00:00:00Z', vehicleId: 'vehicle-1', cursor: 'signed-cursor', limit: 100 };
    await gpsExceptionApi.episodes(filters);
    expect(api.get).toHaveBeenCalledWith('/v1/tracking/gps-exceptions', { params: filters, signal: undefined });
  });

  it('preserves the caller-owned idempotency key and normalized command', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: { episodeId: 'episode-1', status: 'ACKNOWLEDGED', severity: 'WARNING', version: 2, acknowledgedAt: '2026-09-17T00:00:00Z' } });
    await gpsExceptionApi.acknowledge('episode-1', { expectedVersion: 1, reason: 'Reviewed evidence', idempotencyKey: 'stable-key' });
    expect(api.post).toHaveBeenCalledWith('/v1/tracking/gps-exceptions/episode-1/acknowledge', { expectedVersion: 1, reason: 'Reviewed evidence' }, { headers: { 'Idempotency-Key': 'stable-key' } });
  });
});
