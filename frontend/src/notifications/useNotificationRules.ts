import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type {
  CreateNotificationRuleRequest,
  NotificationRule,
  UpdateNotificationRuleRequest,
  DeliveryFilters,
  NotificationDelivery,
  NotificationDeliveryAttempt,
  NotificationEventCatalogueItem,
  NotificationTemplate,
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

export function useNotificationEventCatalogue() {
  return useQuery({
    queryKey: ['notification-event-catalogue'],
    queryFn: async () => (await api.get<NotificationEventCatalogueItem[]>('/notification-event-catalogue')).data,
    staleTime: 5 * 60 * 1000,
  });
}

export function useNotificationTemplates(eventType?: string, channel?: string) {
  return useQuery({
    queryKey: ['notification-templates', eventType, channel],
    queryFn: async () => (await api.get<NotificationTemplate[]>('/notification-templates', {
      params: { eventType, channel },
    })).data,
    enabled: Boolean(eventType && channel),
    staleTime: 5 * 60 * 1000,
  });
}

export function useNotificationDeliveries(filters: DeliveryFilters) {
  return useQuery({
    queryKey: ['notification-deliveries', filters],
    queryFn: async () => (await api.get<NotificationDelivery[]>('/notification-deliveries', {
      params: { ...filters, limit: filters.limit ?? 100 },
    })).data,
  });
}

export function useNotificationDeliveryAttempts(notificationId?: string) {
  return useQuery({
    queryKey: ['notification-delivery-attempts', notificationId],
    queryFn: async () => (await api.get<NotificationDeliveryAttempt[]>(`/notification-deliveries/${notificationId}/attempts`)).data,
    enabled: Boolean(notificationId),
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
