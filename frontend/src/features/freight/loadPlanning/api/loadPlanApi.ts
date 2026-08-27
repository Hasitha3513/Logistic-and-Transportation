import { api } from '../../../../api/client';
import type {
  CreateLoadPlanPayload,
  LoadPlan,
  LoadPlanValidationResponse,
  LoadValidationResultResponse,
  MarkLoadPlanReadyPayload,
  UpdateLoadPlanPayload,
} from '../types/loadPlan';
import type { CargoManifest, ManifestPage } from '../../manifests/types/cargoManifest';
import type { Vehicle } from '../../../fleet/vehicleMaster/types/vehicle';

export const loadPlanApi = {
  list: async () => (await api.get<LoadPlan[]>('/v1/freight/load-plans')).data,
  get: async (id: string) => (await api.get<LoadPlan>(`/v1/freight/load-plans/${id}`)).data,
  create: async (payload: CreateLoadPlanPayload) => (await api.post<LoadPlan>('/v1/freight/load-plans', payload)).data,
  update: async (id: string, payload: UpdateLoadPlanPayload) =>
    (await api.patch<LoadPlan>(`/v1/freight/load-plans/${id}`, payload)).data,
  markReady: async (id: string, payload: MarkLoadPlanReadyPayload) =>
    (await api.post<LoadPlan>(`/v1/freight/load-plans/${id}/ready`, payload)).data,
  validateLayout: async (id: string) =>
    (await api.post<LoadPlanValidationResponse>(`/v1/freight/load-plans/${id}/validate-layout`)).data,
  validateWeightVolume: async (id: string) =>
    (await api.post<LoadValidationResultResponse>(`/v1/freight/load-plans/${id}/validate-weight-volume`)).data,
  vehicles: async () => (await api.get<Vehicle[]>('/vehicles')).data.filter((v) => v.active),
  manifests: async () =>
    (await api.get<ManifestPage>('/v1/freight/manifests', { params: { finalized: true, limit: 100 } })).data.content,
  getManifest: async (id: string) => (await api.get<CargoManifest>(`/v1/freight/manifests/${id}`)).data,
};
