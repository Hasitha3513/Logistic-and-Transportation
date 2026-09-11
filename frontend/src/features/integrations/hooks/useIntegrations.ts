import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { integrationApi } from '../api/integrationApi';
import type { IntegrationPayload } from '../types/integration';

export const integrationKeys = {
  all: ['integrations'] as const,
  list: (page: number) => ['integrations', 'list', page] as const,
  detail: (id?: string) => ['integrations', 'detail', id] as const,
  exchanges: (id?: string) => ['integrations', 'exchanges', id] as const,
};

export const useIntegrations = (page = 0) => useQuery({
  queryKey: integrationKeys.list(page), queryFn: () => integrationApi.list(page),
});
export const useIntegration = (id?: string) => useQuery({
  queryKey: integrationKeys.detail(id), queryFn: () => integrationApi.get(id!), enabled: Boolean(id),
});
export const useIntegrationExchanges = (id?: string, enabled = true) => useQuery({
  queryKey: integrationKeys.exchanges(id), queryFn: () => integrationApi.exchanges(id!), enabled: Boolean(id) && enabled,
});
export function useSaveIntegration(id?: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (payload: IntegrationPayload) => id ? integrationApi.update(id, payload) : integrationApi.create(payload),
    onSuccess: (saved) => { client.setQueryData(integrationKeys.detail(saved.id), saved); void client.invalidateQueries({ queryKey: integrationKeys.all }); },
  });
}
export function useIntegrationAction(id: string, action: 'test' | 'enable' | 'disable') {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (version: number) => action === 'test' ? integrationApi.test(id).then((result) => result.integration)
      : integrationApi[action](id, version),
    onSuccess: (saved) => { client.setQueryData(integrationKeys.detail(id), saved); void client.invalidateQueries({ queryKey: integrationKeys.all }); void client.invalidateQueries({ queryKey: integrationKeys.exchanges(id) }); },
  });
}
