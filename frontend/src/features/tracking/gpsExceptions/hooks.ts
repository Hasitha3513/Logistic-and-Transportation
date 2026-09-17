import { useEffect, useRef } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { gpsExceptionApi } from './api';
import type { AcknowledgementCommand, EpisodeFilters } from './types';

export const gpsExceptionKeys = {
  all: ['tracking', 'gps-exceptions'] as const,
  list: (sessionId: string, filters: EpisodeFilters) => [...gpsExceptionKeys.all, sessionId, 'list', filters] as const,
  detail: (sessionId: string, id: string) => [...gpsExceptionKeys.all, sessionId, 'detail', id] as const,
  evidence: (sessionId: string, id: string, cursor?: string) => [...gpsExceptionKeys.all, sessionId, 'evidence', id, cursor ?? 'first'] as const,
};

export function useGpsExceptionSession(sessionId?: string) {
  const client = useQueryClient();
  const previous = useRef(sessionId);
  useEffect(() => {
    if (previous.current && previous.current !== sessionId) client.removeQueries({ queryKey: gpsExceptionKeys.all });
    previous.current = sessionId;
  }, [client, sessionId]);
}

export function useGpsExceptions(sessionId: string, filters: EpisodeFilters | undefined, enabled: boolean) {
  return useQuery({
    queryKey: gpsExceptionKeys.list(sessionId, filters ?? { from: '', to: '' }),
    queryFn: ({ signal }) => gpsExceptionApi.episodes(filters!, signal),
    enabled: enabled && Boolean(filters),
    retry: false,
  });
}

export function useGpsException(sessionId: string, episodeId?: string, enabled = true) {
  return useQuery({ queryKey: gpsExceptionKeys.detail(sessionId, episodeId ?? ''), queryFn: ({ signal }) => gpsExceptionApi.episode(episodeId!, signal), enabled: enabled && Boolean(episodeId), retry: false });
}

export function useGpsExceptionEvidence(sessionId: string, episodeId?: string, cursor?: string, enabled = true) {
  return useQuery({ queryKey: gpsExceptionKeys.evidence(sessionId, episodeId ?? '', cursor), queryFn: ({ signal }) => gpsExceptionApi.evidence(episodeId!, cursor, signal), enabled: enabled && Boolean(episodeId), retry: false });
}

export function useAcknowledgeGpsException(episodeId?: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (command: AcknowledgementCommand) => gpsExceptionApi.acknowledge(episodeId!, command),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: gpsExceptionKeys.all }); },
  });
}
