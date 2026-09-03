import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import type {
  DeliveryExceptionCase,
  ReportExceptionPayload,
  ResolveExceptionPayload,
  CancelExceptionPayload,
} from '../types/deliveryException';

export const useDeliveryExceptions = (deliveryId?: string) => {
  const queryClient = useQueryClient();

  const exceptionsQuery = useQuery({
    queryKey: ['delivery-exceptions', deliveryId],
    queryFn: async () => {
      if (!deliveryId) return [];
      const res = await axios.get<DeliveryExceptionCase[]>(`/api/v1/deliveries/${deliveryId}/exceptions`);
      return res.data;
    },
    enabled: Boolean(deliveryId),
  });

  const reportExceptionMutation = useMutation({
    mutationFn: async (payload: ReportExceptionPayload) => {
      const res = await axios.post<DeliveryExceptionCase>(
        `/api/v1/deliveries/${deliveryId}/exceptions`,
        payload
      );
      return res.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['delivery-exceptions', deliveryId] });
      void queryClient.invalidateQueries({ queryKey: ['delivery-order', deliveryId] });
    },
  });

  const investigateExceptionMutation = useMutation({
    mutationFn: async ({ exceptionId, expectedVersion }: { exceptionId: string; expectedVersion: number }) => {
      const res = await axios.post<DeliveryExceptionCase>(
        `/api/v1/deliveries/${deliveryId}/exceptions/${exceptionId}/investigate`,
        { expectedVersion }
      );
      return res.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['delivery-exceptions', deliveryId] });
    },
  });

  const resolveExceptionMutation = useMutation({
    mutationFn: async ({
      exceptionId,
      payload,
    }: {
      exceptionId: string;
      payload: ResolveExceptionPayload;
    }) => {
      const res = await axios.post<DeliveryExceptionCase>(
        `/api/v1/deliveries/${deliveryId}/exceptions/${exceptionId}/resolve`,
        payload
      );
      return res.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['delivery-exceptions', deliveryId] });
      void queryClient.invalidateQueries({ queryKey: ['delivery-order', deliveryId] });
    },
  });

  const cancelExceptionMutation = useMutation({
    mutationFn: async ({
      exceptionId,
      payload,
    }: {
      exceptionId: string;
      payload: CancelExceptionPayload;
    }) => {
      const res = await axios.post<DeliveryExceptionCase>(
        `/api/v1/deliveries/${deliveryId}/exceptions/${exceptionId}/cancel`,
        payload
      );
      return res.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['delivery-exceptions', deliveryId] });
    },
  });

  return {
    exceptions: exceptionsQuery.data ?? [],
    isLoading: exceptionsQuery.isLoading,
    isError: exceptionsQuery.isError,
    reportException: reportExceptionMutation.mutateAsync,
    isReporting: reportExceptionMutation.isPending,
    investigateException: investigateExceptionMutation.mutateAsync,
    isInvestigating: investigateExceptionMutation.isPending,
    resolveException: resolveExceptionMutation.mutateAsync,
    isResolving: resolveExceptionMutation.isPending,
    cancelException: cancelExceptionMutation.mutateAsync,
    isCancelling: cancelExceptionMutation.isPending,
    refetch: exceptionsQuery.refetch,
  };
};
