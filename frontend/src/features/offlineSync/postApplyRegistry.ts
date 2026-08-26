import type { OfflineOperation, OfflineSyncOperationResult } from './types';

export type OfflineSyncPostApplyCallback = (
  operation: OfflineOperation,
  result: OfflineSyncOperationResult,
) => void | Promise<void>;

export class OfflineSyncPostApplyRegistry {
  private readonly callbacks = new Map<string, Set<OfflineSyncPostApplyCallback>>();

  register(operationType: OfflineOperation['operationType'], callback: OfflineSyncPostApplyCallback): () => void {
    const callbacks = this.callbacks.get(operationType) ?? new Set<OfflineSyncPostApplyCallback>();
    callbacks.add(callback);
    this.callbacks.set(operationType, callbacks);
    return () => callbacks.delete(callback);
  }

  async notify(operation: OfflineOperation, result: OfflineSyncOperationResult): Promise<void> {
    const callbacks = this.callbacks.get(operation.operationType);
    if (!callbacks) return;
    await Promise.all([...callbacks].map((callback) => callback(operation, result)));
  }
}
