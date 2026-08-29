import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { deliveryOrderApi } from '../api/deliveryOrderApi';
import type { DeliveryOrderFilters, DeliveryOrderPayload } from '../types/deliveryOrder';

export const deliveryOrderKeys = { all: ['delivery-orders'] as const, list: (filters: DeliveryOrderFilters) => ['delivery-orders', 'list', filters] as const, detail: (id?: string) => ['delivery-orders', 'detail', id] as const };
export const useDeliveryOrders = (filters: DeliveryOrderFilters) => useQuery({ queryKey: deliveryOrderKeys.list(filters), queryFn: () => deliveryOrderApi.search(filters) });
export const useDeliveryOrder = (id?: string) => useQuery({ queryKey: deliveryOrderKeys.detail(id), queryFn: () => deliveryOrderApi.get(id!), enabled: Boolean(id) });
export const useDeliveryCustomers = () => useQuery({ queryKey: ['delivery-orders', 'customers'], queryFn: deliveryOrderApi.customers });
export const useDeliveryLocations = () => useQuery({ queryKey: ['delivery-orders', 'locations'], queryFn: deliveryOrderApi.locations });
export function useSaveDeliveryOrder(id?: string) { const client = useQueryClient(); return useMutation({ mutationFn: (payload: DeliveryOrderPayload) => id ? deliveryOrderApi.update(id, payload) : deliveryOrderApi.create(payload), onSuccess: () => client.invalidateQueries({ queryKey: deliveryOrderKeys.all }) }); }
export function useValidateDeliveryReadiness() { const client = useQueryClient(); return useMutation({ mutationFn: ({ id, version }: { id: string; version: number }) => deliveryOrderApi.validateReadiness(id, version), onSuccess: () => client.invalidateQueries({ queryKey: deliveryOrderKeys.all }) }); }
