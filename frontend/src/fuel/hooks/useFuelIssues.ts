import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../api/client';
import type { FuelIssue, FuelIssueHistory, FuelIssuePage, FuelIssuePayload, FuelStation } from '../types';

export interface FuelIssueFilters {
  page: number;
  limit: number;
  status?: string;
  vehicleId?: string;
  tripId?: string;
  voucherNumber?: string;
  fromDate?: string;
  toDate?: string;
}

export const useFuelIssues = (filters: FuelIssueFilters) => useQuery({
  queryKey: ['fuel-issues', filters],
  queryFn: async () => (await api.get<FuelIssuePage>('/fuel-issues', { params: filters })).data,
  placeholderData: (previous) => previous,
});

export const useFuelIssue = (id?: string) => useQuery({
  queryKey: ['fuel-issues', id],
  queryFn: async () => (await api.get<FuelIssue>(`/fuel-issues/${id}`)).data,
  enabled: Boolean(id),
});

export const useFuelIssueHistory = (id?: string) => useQuery({
  queryKey: ['fuel-issues', id, 'history'],
  queryFn: async () => (await api.get<FuelIssueHistory[]>(`/fuel-issues/${id}/history`)).data,
  enabled: Boolean(id),
});

export const useFuelStations = () => useQuery({
  queryKey: ['fuel-stations', 'active'],
  queryFn: async () => (await api.get<FuelStation[]>('/fuel-stations', { params: { active: true } })).data,
});

export function useSaveFuelIssue(id?: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (payload: FuelIssuePayload) => id
      ? (await api.put<FuelIssue>(`/fuel-issues/${id}`, payload)).data
      : (await api.post<FuelIssue>('/fuel-issues', payload)).data,
    onSuccess: async () => client.invalidateQueries({ queryKey: ['fuel-issues'] }),
  });
}

export function useFuelAction(id: string, action: 'submit' | 'authorize' | 'issue' | 'cancel') {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (body?: object) => (await api.post<FuelIssue>(`/fuel-issues/${id}/${action}`, body)).data,
    onSuccess: async () => client.invalidateQueries({ queryKey: ['fuel-issues'] }),
  });
}

export const useSubmitFuelIssue = (id: string) => useFuelAction(id, 'submit');
export const useAuthorizeFuelIssue = (id: string) => useFuelAction(id, 'authorize');
export const useIssueFuel = (id: string) => useFuelAction(id, 'issue');
export const useCancelFuelIssue = (id: string) => useFuelAction(id, 'cancel');
