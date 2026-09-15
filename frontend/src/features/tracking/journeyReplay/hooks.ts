import { useQuery } from '@tanstack/react-query';
import { journeyReplayApi } from './api';
import type { ReplayQuery } from './types';

export const replayKeys = {
  all: ['tracking', 'journey-replay'] as const,
  points: (query: ReplayQuery) => [...replayKeys.all, 'points', query] as const,
  stops: (query: ReplayQuery) => [...replayKeys.all, 'stops', query] as const,
};

export function useJourneyReplay(query?: ReplayQuery) {
  const stopQuery = query ? { ...query, cursor: undefined } : undefined;
  const points = useQuery({ queryKey: replayKeys.points(query ?? { vehicleId: '' }), queryFn: ({ signal }) => journeyReplayApi.points(query!, signal), enabled: Boolean(query), retry: false });
  const stops = useQuery({ queryKey: replayKeys.stops(stopQuery ?? { vehicleId: '' }), queryFn: ({ signal }) => journeyReplayApi.stops(stopQuery!, signal), enabled: Boolean(stopQuery), retry: false });
  return { points, stops };
}
