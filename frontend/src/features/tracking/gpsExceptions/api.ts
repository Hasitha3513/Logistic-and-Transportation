import { api } from '../../../api/client';
import type { Acknowledgement, AcknowledgementCommand, CursorPage, EpisodeFilters, GpsExceptionEpisode, GpsExceptionEvidence } from './types';

export const gpsExceptionApi = {
  episodes: async (filters: EpisodeFilters, signal?: AbortSignal) =>
    (await api.get<CursorPage<GpsExceptionEpisode>>('/v1/tracking/gps-exceptions', { params: filters, signal })).data,
  episode: async (episodeId: string, signal?: AbortSignal) =>
    (await api.get<GpsExceptionEpisode>(`/v1/tracking/gps-exceptions/${episodeId}`, { signal })).data,
  evidence: async (episodeId: string, cursor?: string, signal?: AbortSignal) =>
    (await api.get<CursorPage<GpsExceptionEvidence>>(`/v1/tracking/gps-exceptions/${episodeId}/evidence`, { params: { cursor, limit: 100 }, signal })).data,
  acknowledge: async (episodeId: string, command: AcknowledgementCommand) =>
    (await api.post<Acknowledgement>(`/v1/tracking/gps-exceptions/${episodeId}/acknowledge`, {
      expectedVersion: command.expectedVersion,
      reason: command.reason,
    }, { headers: { 'Idempotency-Key': command.idempotencyKey } })).data,
};
