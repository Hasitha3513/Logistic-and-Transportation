import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { freightOrderApi } from '../api/freightOrderApi';
import type { FreightOrderFilters, FreightOrderPayload } from '../types/freightOrder';

export const freightOrderKeys = { all: ['freight-orders'] as const, detail: (id: string) => ['freight-orders', id] as const };
export const useFreightOrders = (filters: FreightOrderFilters) => useQuery({ queryKey: [...freightOrderKeys.all, filters], queryFn: () => freightOrderApi.search(filters), placeholderData: (old) => old });
export const useFreightOrder = (id?: string) => useQuery({ queryKey: freightOrderKeys.detail(id ?? ''), queryFn: () => freightOrderApi.get(id!), enabled: Boolean(id) });
export const useFreightCustomers = () => useQuery({ queryKey: ['freight-orders', 'customers'], queryFn: freightOrderApi.customers, retry: false });
export const useFreightLocations = () => useQuery({ queryKey: ['freight-orders', 'locations'], queryFn: freightOrderApi.locations, retry: false });
export function useSaveFreightOrder(id?: string) {
  const client = useQueryClient();
  return useMutation({ mutationFn: (payload: FreightOrderPayload) => id ? freightOrderApi.update(id, payload) : freightOrderApi.create(payload),
    onSuccess: async (saved) => { client.setQueryData(freightOrderKeys.detail(saved.id), saved); await client.invalidateQueries({ queryKey: freightOrderKeys.all }); } });
}
