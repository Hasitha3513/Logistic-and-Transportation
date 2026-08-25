import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { routeApi } from '../api/routeApi';
import type { CreateDisruptionInput } from '../types/route';

export const routeKeys = {
  all: ['routes'] as const,
  revisions: (routeId: string) => ['routes', routeId, 'revisions'] as const,
  disruptions: (routeId: string) => ['routes', routeId, 'disruptions'] as const,
  activeDisruptions: ['routes', 'disruptions', 'active'] as const,
  performance: (routeId: string, from?: string, to?: string) => ['routes', routeId, 'performance', { from, to }] as const,
};

export function useRouteRevisions(routeId?: string) {
  return useQuery({
    queryKey: routeId ? routeKeys.revisions(routeId) : ['routes', 'no-id', 'revisions'],
    queryFn: () => routeApi.listRevisions(routeId!),
    enabled: Boolean(routeId),
  });
}

export function useRouteDisruptions(routeId?: string) {
  return useQuery({
    queryKey: routeId ? routeKeys.disruptions(routeId) : ['routes', 'no-id', 'disruptions'],
    queryFn: () => routeApi.listDisruptions(routeId!),
    enabled: Boolean(routeId),
  });
}

export function useActiveDisruptions() {
  return useQuery({
    queryKey: routeKeys.activeDisruptions,
    queryFn: () => routeApi.listActiveDisruptions(),
  });
}

export function useCreateRouteDisruption(routeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateDisruptionInput) => routeApi.createDisruption(routeId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: routeKeys.disruptions(routeId) });
      void queryClient.invalidateQueries({ queryKey: routeKeys.activeDisruptions });
    },
  });
}

export function useResolveRouteDisruption(routeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (disruptionId: string) => routeApi.resolveDisruption(routeId, disruptionId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: routeKeys.disruptions(routeId) });
      void queryClient.invalidateQueries({ queryKey: routeKeys.activeDisruptions });
    },
  });
}

export function useOptimizeRoute(routeId: string) {
  return useMutation({
    mutationFn: () => routeApi.optimize(routeId),
  });
}

export function useApplyOptimization(routeId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (optimizedStopLocationIds: string[]) =>
      routeApi.applyOptimization(routeId, optimizedStopLocationIds),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: routeKeys.all });
      void queryClient.invalidateQueries({ queryKey: routeKeys.revisions(routeId) });
    },
  });
}

export function useRoutePerformance(routeId?: string, from?: string, to?: string) {
  return useQuery({
    queryKey: routeId ? routeKeys.performance(routeId, from, to) : ['routes', 'no-id', 'performance'],
    queryFn: () => routeApi.getPerformance(routeId!, from, to),
    enabled: Boolean(routeId),
  });
}
