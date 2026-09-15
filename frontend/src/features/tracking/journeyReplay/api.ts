import { api } from '../../../api/client';
import type { ReplayPointsPage, ReplayQuery, ReplayStopsPage } from './types';

export const journeyReplayApi = {
  points: async (query: ReplayQuery, signal?: AbortSignal) =>
    (await api.post<ReplayPointsPage>('/v1/tracking/journey-replays/points/query', query, { signal })).data,
  stops: async (query: ReplayQuery, signal?: AbortSignal) =>
    (await api.post<ReplayStopsPage>('/v1/tracking/journey-replays/stops/query', { ...query, limit: 100 }, { signal })).data,
};
