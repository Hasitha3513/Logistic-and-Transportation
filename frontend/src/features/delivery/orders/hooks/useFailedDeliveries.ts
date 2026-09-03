import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { deliveryOrderApi } from '../api/deliveryOrderApi';
import type {
  RecordFailedAttemptPayload,
  RecordContactAttemptPayload,
  EscalateDeliveryPayload,
  UpdateEscalationPayload,
  ReturnToBasePayload
} from '../types/failedDelivery';

export const useFailedDeliveries = (deliveryId?: string) => {
  const queryClient = useQueryClient();

  const attemptsQuery = useQuery({
    queryKey: ['delivery-attempts', deliveryId],
    queryFn: () => (deliveryId ? deliveryOrderApi.getAttempts(deliveryId) : Promise.reject('No delivery ID')),
    enabled: Boolean(deliveryId),
  });

  const recordFailedAttemptMutation = useMutation({
    mutationFn: (payload: RecordFailedAttemptPayload) => {
      if (!deliveryId) throw new Error('No delivery ID');
      return deliveryOrderApi.recordFailedAttempt(deliveryId, payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['delivery-attempts', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-orders', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-orders'] });
    },
  });

  const recordContactAttemptMutation = useMutation({
    mutationFn: ({ attemptId, payload }: { attemptId: string; payload: RecordContactAttemptPayload }) => {
      if (!deliveryId) throw new Error('No delivery ID');
      return deliveryOrderApi.recordContactAttempt(deliveryId, attemptId, payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['delivery-attempts', deliveryId] });
    },
  });

  const escalateMutation = useMutation({
    mutationFn: (payload: EscalateDeliveryPayload) => {
      if (!deliveryId) throw new Error('No delivery ID');
      return deliveryOrderApi.escalateDelivery(deliveryId, payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['delivery-attempts', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-orders', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-orders'] });
    },
  });

  const updateEscalationMutation = useMutation({
    mutationFn: ({ escalationId, payload }: { escalationId: string; payload: UpdateEscalationPayload }) => {
      if (!deliveryId) throw new Error('No delivery ID');
      return deliveryOrderApi.updateEscalation(deliveryId, escalationId, payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['delivery-attempts', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-orders', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-orders'] });
    },
  });

  const returnToBaseMutation = useMutation({
    mutationFn: (payload: ReturnToBasePayload) => {
      if (!deliveryId) throw new Error('No delivery ID');
      return deliveryOrderApi.returnToBase(deliveryId, payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['delivery-attempts', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-orders', deliveryId] });
      queryClient.invalidateQueries({ queryKey: ['delivery-orders'] });
    },
  });

  return {
    history: attemptsQuery.data,
    isLoading: attemptsQuery.isLoading,
    isError: attemptsQuery.isError,
    refetchHistory: attemptsQuery.refetch,
    recordFailedAttempt: recordFailedAttemptMutation.mutateAsync,
    isRecordingAttempt: recordFailedAttemptMutation.isPending,
    recordContactAttempt: recordContactAttemptMutation.mutateAsync,
    isRecordingContact: recordContactAttemptMutation.isPending,
    escalateDelivery: escalateMutation.mutateAsync,
    isEscalating: escalateMutation.isPending,
    updateEscalation: updateEscalationMutation.mutateAsync,
    isUpdatingEscalation: updateEscalationMutation.isPending,
    returnToBase: returnToBaseMutation.mutateAsync,
    isReturningToBase: returnToBaseMutation.isPending,
  };
};
