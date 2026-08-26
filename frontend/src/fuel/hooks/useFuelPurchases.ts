import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../api/client';
import type { FuelPrice, FuelPricePayload, FuelPurchase, FuelPurchaseHistory, FuelPurchasePage, FuelPurchasePayload, Vendor } from '../purchaseTypes';

export interface PurchaseFilters { page: number; limit: number; search?: string; vendorId?: string; fuelType?: string; status?: string; reconciliationStatus?: string; fromDate?: string; toDate?: string }
export const useFuelPurchases = (filters: PurchaseFilters) => useQuery({ queryKey: ['fuel-purchases', filters], queryFn: async () => (await api.get<FuelPurchasePage>('/fuel-purchases', { params: filters })).data, placeholderData: (old) => old });
export const useFuelPurchase = (id?: string) => useQuery({ queryKey: ['fuel-purchases', id], queryFn: async () => (await api.get<FuelPurchase>(`/fuel-purchases/${id}`)).data, enabled: Boolean(id) });
export const useFuelPurchaseHistory = (id?: string) => useQuery({ queryKey: ['fuel-purchases', id, 'history'], queryFn: async () => (await api.get<FuelPurchaseHistory[]>(`/fuel-purchases/${id}/history`)).data, enabled: Boolean(id) });
export const useFuelVendors = () => useQuery({ queryKey: ['fuel-vendors'], queryFn: async () => (await api.get<Vendor[]>('/vendors', { params: { active: true } })).data });
export const useFuelPrices = () => useQuery({ queryKey: ['fuel-prices'], queryFn: async () => (await api.get<FuelPrice[]>('/fuel-prices')).data });

export function useSaveFuelPurchase(id?: string) { const client = useQueryClient(); return useMutation({ mutationFn: async (payload: FuelPurchasePayload) => id ? (await api.put<FuelPurchase>(`/fuel-purchases/${id}`, payload)).data : (await api.post<FuelPurchase>('/fuel-purchases', payload)).data, onSuccess: async () => client.invalidateQueries({ queryKey: ['fuel-purchases'] }) }); }
export const useCreateFuelPurchase = () => useSaveFuelPurchase();
export const useUpdateFuelPurchase = (id: string) => useSaveFuelPurchase(id);
export function useFuelPurchaseAction(id: string, action: 'submit'|'approve'|'receive'|'reconcile'|'cancel') { const client = useQueryClient(); return useMutation({ mutationFn: async (body?: object) => (await api.post<FuelPurchase>(`/fuel-purchases/${id}/${action}`, body)).data, onSuccess: async () => client.invalidateQueries({ queryKey: ['fuel-purchases'] }) }); }
export const useSubmitFuelPurchase = (id: string) => useFuelPurchaseAction(id, 'submit');
export const useApproveFuelPurchase = (id: string) => useFuelPurchaseAction(id, 'approve');
export const useReceiveFuelPurchase = (id: string) => useFuelPurchaseAction(id, 'receive');
export const useReconcileFuelPurchase = (id: string) => useFuelPurchaseAction(id, 'reconcile');
export const useCancelFuelPurchase = (id: string) => useFuelPurchaseAction(id, 'cancel');
export function useSaveFuelPrice(id?: string) { const client = useQueryClient(); return useMutation({ mutationFn: async (payload: FuelPricePayload) => id ? (await api.put<FuelPrice>(`/fuel-prices/${id}`, payload)).data : (await api.post<FuelPrice>('/fuel-prices', payload)).data, onSuccess: async () => client.invalidateQueries({ queryKey: ['fuel-prices'] }) }); }
export const useCreateFuelPrice = () => useSaveFuelPrice();
export const useUpdateFuelPrice = (id: string) => useSaveFuelPrice(id);
