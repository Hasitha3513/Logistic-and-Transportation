import { useQueryClient } from '@tanstack/react-query';
import { useOfflineSync } from './OfflineSyncProvider';
import type { OfflineOperation } from './types';

export function useOfflineOperationActions() {
  const queryClient = useQueryClient();
  const { retryOperation, discardOperation } = useOfflineSync();

  const refresh = async (operation: OfflineOperation) => {
    if (operation.aggregateType === 'VEHICLE') {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['vehicle-readings', operation.aggregateId] }),
        queryClient.invalidateQueries({ queryKey: ['vehicle-readings-latest', operation.aggregateId] }),
        queryClient.invalidateQueries({ queryKey: ['vehicle-mileage', operation.aggregateId] }),
        queryClient.invalidateQueries({ queryKey: ['vehicles-page'] }),
        queryClient.invalidateQueries({ queryKey: ['vehicles-page', operation.aggregateId] }),
      ]);
    } else {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trip-operational-events', operation.aggregateId] }),
        queryClient.invalidateQueries({ queryKey: ['trip-history', operation.aggregateId] }),
        queryClient.invalidateQueries({ queryKey: ['trip', operation.aggregateId] }),
        queryClient.invalidateQueries({ queryKey: ['trips'] }),
      ]);
    }
  };

  return {
    retry: (operation: OfflineOperation) => retryOperation(operation.operationId),
    discard: (operation: OfflineOperation) => discardOperation(operation.operationId),
    refresh,
    open: (operation: OfflineOperation) => {
      const path = operation.aggregateType === 'TRIP'
        ? `/trips/${operation.aggregateId}`
        : `/fleet/vehicles?vehicleId=${encodeURIComponent(operation.aggregateId)}`;
      window.history.pushState({}, '', path);
      window.dispatchEvent(new PopStateEvent('popstate'));
    },
  };
}
