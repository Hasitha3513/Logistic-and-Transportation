import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { vehicleApi } from '../api/vehicleApi';
import type { VehicleInput } from '../types/vehicle';

export const vehicleKeys = {
  all: ['vehicles-page'] as const,
  lists: () => vehicleKeys.all,
  details: () => [...vehicleKeys.all, 'detail'] as const,
  detail: (id: string) => [...vehicleKeys.all, id] as const,
  documents: (id: string) => [...vehicleKeys.all, id, 'compliance'] as const,
  categories: ['vehicle-master', 'categories'] as const,
  types: ['vehicle-master', 'types'] as const,
};

export function useVehicles() {
  return useQuery({ queryKey: vehicleKeys.lists(), queryFn: vehicleApi.list });
}

export function useVehicle(id?: string) {
  return useQuery({
    queryKey: vehicleKeys.detail(id ?? 'none'),
    queryFn: () => vehicleApi.get(id!),
    enabled: Boolean(id),
  });
}

export function useVehicleReferences(enabled = true) {
  const categories = useQuery({ queryKey: vehicleKeys.categories, queryFn: vehicleApi.listCategories, enabled });
  const types = useQuery({ queryKey: vehicleKeys.types, queryFn: vehicleApi.listTypes, enabled });
  return { categories, types };
}

export function useVehicleDocuments(vehicleId?: string) {
  return useQuery({
    queryKey: vehicleKeys.documents(vehicleId ?? 'none'),
    queryFn: () => vehicleApi.listDocuments(vehicleId!),
    enabled: Boolean(vehicleId),
  });
}

export function useCreateVehicle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: vehicleApi.create,
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: vehicleKeys.all }),
  });
}

export function useUpdateVehicle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: VehicleInput }) => vehicleApi.update(id, input),
    onSuccess: async (vehicle) => {
      queryClient.setQueryData(vehicleKeys.detail(vehicle.id), vehicle);
      await queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}

export function useDeactivateVehicle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: vehicleApi.deactivate,
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: vehicleKeys.all }),
  });
}
