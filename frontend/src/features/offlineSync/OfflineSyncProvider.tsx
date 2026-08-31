import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react';
import { useAuth } from '../../auth/AuthContext';
import type { OfflineSyncPostApplyCallback } from './postApplyRegistry';
import { OfflineSyncCoordinator, type OfflineSyncCoordinatorState } from './syncCoordinator';
import { IndexedDbOfflineOperationStorage } from './storage';
import type { OfflineAggregateType, OfflineOperation, OfflineOperationInput } from './types';

export interface OfflineSyncContextValue extends OfflineSyncCoordinatorState {
  syncNow: () => Promise<void>;
  registerPostApply: (
    operationType: OfflineOperation['operationType'],
    callback: OfflineSyncPostApplyCallback,
  ) => () => void;
  enqueueOperation: (input: OfflineOperationInput) => Promise<OfflineOperation>;
  getOperationsForAggregate: (
    aggregateType: OfflineAggregateType,
    aggregateId: string,
  ) => Promise<OfflineOperation[]>;
  getOperations: () => Promise<OfflineOperation[]>;
  retryOperation: (operationId: string) => Promise<OfflineOperation>;
  discardOperation: (operationId: string) => Promise<boolean>;
  operationsRevision: number;
}

const OfflineSyncContext = createContext<OfflineSyncContextValue | undefined>(undefined);

export function OfflineSyncProvider({ children }: PropsWithChildren) {
  const { user } = useAuth();
  const [operationsRevision, setOperationsRevision] = useState(0);
  const resources = useMemo(() => {
    const storage = new IndexedDbOfflineOperationStorage({
      onChange: () => setOperationsRevision((current) => current + 1),
    });
    return { storage, coordinator: new OfflineSyncCoordinator({ storage }) };
  }, []);
  const coordinator = resources.coordinator;
  const [state, setState] = useState(coordinator.getState());

  useEffect(() => coordinator.subscribe(setState), [coordinator]);
  useEffect(() => {
    if (user?.id) void coordinator.activate(user.id);
    else coordinator.deactivate();
  }, [coordinator, user?.id]);
  useEffect(() => () => coordinator.dispose(), [coordinator]);

  return (
    <OfflineSyncContext.Provider
      value={{
        ...state,
        syncNow: () => coordinator.syncNow(),
        registerPostApply: (operationType, callback) => coordinator.registerPostApply(operationType, callback),
        enqueueOperation: async (input) => {
          const operation = await resources.storage.enqueue(input);
          void coordinator.syncNow();
          return operation;
        },
        getOperationsForAggregate: (aggregateType, aggregateId) => {
          if (!user?.id) return Promise.resolve([]);
          return resources.storage.getForAggregate(user.id, aggregateType, aggregateId);
        },
        getOperations: () => user?.id
          ? resources.storage.getAllForOwner(user.id)
          : Promise.resolve([]),
        retryOperation: async (operationId) => {
          if (!user?.id) throw new Error('A signed-in user is required');
          const operation = await resources.storage.retryOperation(user.id, operationId);
          void coordinator.syncNow();
          return operation;
        },
        discardOperation: (operationId) => {
          if (!user?.id) return Promise.resolve(false);
          return resources.storage.remove(user.id, operationId);
        },
        operationsRevision,
      }}
    >
      {children}
    </OfflineSyncContext.Provider>
  );
}

export function useOfflineSync(): OfflineSyncContextValue {
  const context = useContext(OfflineSyncContext);
  if (!context) throw new Error('useOfflineSync must be used inside OfflineSyncProvider');
  return context;
}

export function useOptionalOfflineSync(): OfflineSyncContextValue | undefined {
  return useContext(OfflineSyncContext);
}
