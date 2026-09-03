import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { operationalExceptionApi } from '../api/operationalExceptionApi';

export const operationalExceptionKeys = {
  all: ['operational-exceptions'] as const,
  list: (filters: object) => ['operational-exceptions', 'list', filters] as const,
  detail: (id?: string) => ['operational-exceptions', 'detail', id] as const,
  history: (id?: string) => ['operational-exceptions', 'history', id] as const,
};

export function useOperationalExceptions(filters: Record<string, string | number | undefined>) {
  return useQuery({ queryKey: operationalExceptionKeys.list(filters), queryFn: () => operationalExceptionApi.list(filters) });
}
export function useOperationalException(id?: string) {
  return useQuery({ queryKey: operationalExceptionKeys.detail(id), queryFn: () => operationalExceptionApi.get(id!), enabled: Boolean(id) });
}
export function useOperationalExceptionHistory(id?: string, enabled = true) {
  return useQuery({ queryKey: operationalExceptionKeys.history(id), queryFn: () => operationalExceptionApi.history(id!), enabled: Boolean(id) && enabled });
}
export function useOperationalExceptionCommand(id?: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ action, payload }: { action: string; payload: unknown }) => operationalExceptionApi.command(id!, action, payload),
    onSuccess: (detail) => {
      client.setQueryData(operationalExceptionKeys.detail(detail.exceptionCase.id), detail);
      void client.invalidateQueries({ queryKey: operationalExceptionKeys.all });
      void client.invalidateQueries({ queryKey: operationalExceptionKeys.history(detail.exceptionCase.id) });
    },
  });
}
export function useCorrectiveActionCommand(id?: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ actionId, action, version }: { actionId: string; action: 'start' | 'complete'; version: number }) =>
      operationalExceptionApi.actionCommand(id!, actionId, action, version),
    onSuccess: (detail) => {
      client.setQueryData(operationalExceptionKeys.detail(detail.exceptionCase.id), detail);
      void client.invalidateQueries({ queryKey: operationalExceptionKeys.all });
      void client.invalidateQueries({ queryKey: operationalExceptionKeys.history(detail.exceptionCase.id) });
    },
  });
}
