import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../api/client';
import type {
  BunkerStockMovementPage,
  BunkerTank,
  BunkerTankBalance,
  BunkerTankCreatePayload,
  BunkerTankUpdatePayload,
  BunkerTransferPayload,
  DipReading,
  DipReadingPayload,
  StockAdjustment,
  StockAdjustmentPayload,
} from '../bunkerTypes';

export interface BunkerTankFilters {
  fuelStationId?: string;
  fuelType?: string;
  active?: boolean;
}

export const useBunkerTanks = (filters?: BunkerTankFilters) =>
  useQuery({
    queryKey: ['bunker-tanks', filters],
    queryFn: async () => (await api.get<BunkerTank[]>('/bunker-tanks', { params: filters })).data,
  });

export const useBunkerTank = (id?: string) =>
  useQuery({
    queryKey: ['bunker-tanks', id],
    queryFn: async () => (await api.get<BunkerTank>(`/bunker-tanks/${id}`)).data,
    enabled: Boolean(id),
  });

export const useBunkerBalance = (id?: string) =>
  useQuery({
    queryKey: ['bunker-tanks', id, 'balance'],
    queryFn: async () => (await api.get<BunkerTankBalance>(`/bunker-tanks/${id}/balance`)).data,
    enabled: Boolean(id),
  });

export const useBunkerMovements = (id?: string, page = 0, limit = 20) =>
  useQuery({
    queryKey: ['bunker-tanks', id, 'movements', { page, limit }],
    queryFn: async () =>
      (
        await api.get<BunkerStockMovementPage>(`/bunker-tanks/${id}/movements`, {
          params: { page, limit },
        })
      ).data,
    enabled: Boolean(id),
    placeholderData: (previous) => previous,
  });

export const useBunkerDipReadings = (id?: string) =>
  useQuery({
    queryKey: ['bunker-tanks', id, 'dip-readings'],
    queryFn: async () => (await api.get<DipReading[]>(`/bunker-tanks/${id}/dip-readings`)).data,
    enabled: Boolean(id),
  });

export function useCreateBunkerTank() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (payload: BunkerTankCreatePayload) =>
      (await api.post<BunkerTank>('/bunker-tanks', payload)).data,
    onSuccess: async () => client.invalidateQueries({ queryKey: ['bunker-tanks'] }),
  });
}

export function useUpdateBunkerTank(id: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (payload: BunkerTankUpdatePayload) =>
      (await api.put<BunkerTank>(`/bunker-tanks/${id}`, payload)).data,
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ['bunker-tanks'] }),
        client.invalidateQueries({ queryKey: ['bunker-tanks', id] }),
        client.invalidateQueries({ queryKey: ['bunker-tanks', id, 'balance'] }),
      ]);
    },
  });
}

export function useSetOpeningBalance(id: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (payload: { openingBalanceLiters: number; reason?: string }) =>
      (await api.post<BunkerTank>(`/bunker-tanks/${id}/opening-balance`, payload)).data,
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ['bunker-tanks'] }),
        client.invalidateQueries({ queryKey: ['bunker-tanks', id] }),
        client.invalidateQueries({ queryKey: ['bunker-tanks', id, 'balance'] }),
        client.invalidateQueries({ queryKey: ['bunker-tanks', id, 'movements'] }),
      ]);
    },
  });
}

export function useRecordDipReading(tankId: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (payload: DipReadingPayload) =>
      (await api.post<DipReading>(`/bunker-tanks/${tankId}/dip-readings`, payload)).data,
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ['bunker-tanks', tankId, 'balance'] }),
        client.invalidateQueries({ queryKey: ['bunker-tanks', tankId, 'dip-readings'] }),
      ]);
    },
  });
}

export function useAdjustBunkerStock(tankId: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (payload: StockAdjustmentPayload) =>
      (await api.post<StockAdjustment>(`/bunker-tanks/${tankId}/adjustments`, payload)).data,
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ['bunker-tanks'] }),
        client.invalidateQueries({ queryKey: ['bunker-tanks', tankId] }),
        client.invalidateQueries({ queryKey: ['bunker-tanks', tankId, 'balance'] }),
        client.invalidateQueries({ queryKey: ['bunker-tanks', tankId, 'movements'] }),
        client.invalidateQueries({ queryKey: ['bunker-tanks', tankId, 'dip-readings'] }),
      ]);
    },
  });
}

export function useTransferBunkerStock() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (payload: BunkerTransferPayload) =>
      (await api.post<void>('/bunker-transfers', payload)).data,
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ['bunker-tanks'] });
    },
  });
}