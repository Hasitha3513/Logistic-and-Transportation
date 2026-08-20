import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type { NotificationItem, UnreadCountResponse } from './types';

export function useNotifications(limit = 50) {
  return useQuery({
    queryKey: ['notifications', limit],
    queryFn: async () => {
      const response = await api.get<NotificationItem[]>(`/notifications?limit=${limit}`);
      return response.data;
    },
    refetchInterval: 30000, // 30s background poll
  });
}

export function useUnreadNotificationCount() {
  return useQuery({
    queryKey: ['notifications-unread-count'],
    queryFn: async () => {
      const response = await api.get<UnreadCountResponse>('/notifications/unread-count');
      return response.data.unreadCount;
    },
    refetchInterval: 15000, // 15s background poll
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (notificationId: string) => {
      const response = await api.patch<NotificationItem>(`/notifications/${notificationId}/read`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count'] });
    },
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await api.patch('/notifications/read-all');
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count'] });
    },
  });
}
