import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { loadPlanApi } from '../api/loadPlanApi';
import type { CreateLoadPlanPayload, UpdateLoadPlanPayload } from '../types/loadPlan';

export const loadPlanKeys = {
  all: ['load-plans'] as const,
  detail: (id: string) => ['load-plans', id] as const,
  layoutValidation: (id: string) => ['load-plans', id, 'layout-validation'] as const,
  weightVolumeValidation: (id: string) => ['load-plans', id, 'weight-volume-validation'] as const,
  vehicles: ['load-plans', 'vehicles'] as const,
  finalizedManifests: ['load-plans', 'finalized-manifests'] as const,
  manifestDetail: (id: string) => ['cargo-manifests', id] as const,
};

export const useLoadPlans = () =>
  useQuery({
    queryKey: loadPlanKeys.all,
    queryFn: loadPlanApi.list,
  });

export const useLoadPlan = (id?: string) =>
  useQuery({
    queryKey: loadPlanKeys.detail(id ?? ''),
    queryFn: () => loadPlanApi.get(id!),
    enabled: Boolean(id),
  });

export const useAvailableVehicles = () =>
  useQuery({
    queryKey: loadPlanKeys.vehicles,
    queryFn: loadPlanApi.vehicles,
  });

export const useFinalizedManifests = () =>
  useQuery({
    queryKey: loadPlanKeys.finalizedManifests,
    queryFn: loadPlanApi.manifests,
  });

export const useManifestForPlanning = (manifestId?: string) =>
  useQuery({
    queryKey: loadPlanKeys.manifestDetail(manifestId ?? ''),
    queryFn: () => loadPlanApi.getManifest(manifestId!),
    enabled: Boolean(manifestId),
  });

export function useSaveLoadPlan(id?: string) {
  const client = useQueryClient();
  const sync = async (savedId?: string) => {
    const targetId = savedId || id;
    if (targetId) {
      await client.invalidateQueries({ queryKey: loadPlanKeys.detail(targetId) });
      await client.invalidateQueries({ queryKey: loadPlanKeys.layoutValidation(targetId) });
      await client.invalidateQueries({ queryKey: loadPlanKeys.weightVolumeValidation(targetId) });
    }
    await client.invalidateQueries({ queryKey: loadPlanKeys.all });
  };

  return {
    create: useMutation({
      mutationFn: (payload: CreateLoadPlanPayload) => loadPlanApi.create(payload),
      onSuccess: async (saved) => {
        await sync(saved.id);
      },
    }),
    update: useMutation({
      mutationFn: (payload: UpdateLoadPlanPayload) => loadPlanApi.update(id!, payload),
      onSuccess: async (saved) => {
        await sync(saved.id);
      },
    }),
    validateLayout: useMutation({
      mutationFn: () => loadPlanApi.validateLayout(id!),
    }),
    validateWeightVolume: useMutation({
      mutationFn: () => loadPlanApi.validateWeightVolume(id!),
    }),
  };
}
