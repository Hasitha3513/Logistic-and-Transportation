import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { deliveryOrderApi } from '../api/deliveryOrderApi';
import type {
  ScheduleRedeliveryPayload,
  RescheduleRedeliveryPayload,
  RedeliverySuggestionPayload
} from '../types/redelivery';

export const useRedelivery = (deliveryId: string) => {
  const queryClient = useQueryClient();

  const historyQuery = useQuery({
    queryKey: ['delivery-redelivery-history', deliveryId],
    queryFn: () => deliveryOrderApi.getRedeliveryHistory(deliveryId),
    enabled: !!deliveryId,
  });

  const suggestionsMutation = useMutation({
    mutationFn: (payload?: RedeliverySuggestionPayload) =>
      deliveryOrderApi.getRedeliverySuggestions(deliveryId, payload),
  });

  const scheduleMutation = useMutation({
    mutationFn: (payload: ScheduleRedeliveryPayload) =>
      deliveryOrderApi.scheduleRedelivery(deliveryId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['delivery-orders'] });
      queryClient.invalidateQueries({ queryKey: ['delivery-order', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-redelivery-history', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-attempts', deliveryId] });
    },
  });

  const rescheduleMutation = useMutation({
    mutationFn: (payload: RescheduleRedeliveryPayload) =>
      deliveryOrderApi.rescheduleRedelivery(deliveryId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['delivery-orders'] });
      queryClient.invalidateQueries({ queryKey: ['delivery-order', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-redelivery-history', deliveryId] });
    },
  });

  return {
    history: historyQuery.data || [],
    isLoadingHistory: historyQuery.isLoading,
    refetchHistory: historyQuery.refetch,
    getSuggestions: suggestionsMutation.mutateAsync,
    isGettingSuggestions: suggestionsMutation.isPending,
    suggestions: suggestionsMutation.data || [],
    scheduleRedelivery: scheduleMutation.mutateAsync,
    isScheduling: scheduleMutation.isPending,
    rescheduleRedelivery: rescheduleMutation.mutateAsync,
    isRescheduling: rescheduleMutation.isPending,
  };
};
