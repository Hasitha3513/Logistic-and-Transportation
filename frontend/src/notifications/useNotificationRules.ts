import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type {
  CreateNotificationRuleRequest,
  NotificationRule,
  UpdateNotificationRuleRequest,
} from './types';

export function useNotificationRules() {
  return useQuery({
    queryKey: ['notification-rules'],
    queryFn: async () => {
      const response = await api.get<NotificationRule[]>('/notification-rules');
      return response.data;
    },
  });
}

export function useCreateNotificationRule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateNotificationRuleRequest) => {
      const response = await api.post<NotificationRule>('/notification-rules', payload);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-rules'] });
    },
  });
}

export function useUpdateNotificationRule(ruleId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: UpdateNotificationRuleRequest) => {
      const response = await api.put<NotificationRule>(`/notification-rules/${ruleId}`, payload);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-rules'] });
    },
  });
}

export function useEnableNotificationRule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (ruleId: string) => {
      const response = await api.patch<NotificationRule>(`/notification-rules/${ruleId}/enable`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-rules'] });
    },
  });
}

export function useDisableNotificationRule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (ruleId: string) => {
      const response = await api.patch<NotificationRule>(`/notification-rules/${ruleId}/disable`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-rules'] });
    },
  });
}

export function useDeleteNotificationRule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (ruleId: string) => {
      await api.delete(`/notification-rules/${ruleId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-rules'] });
    },
  });
}
