import { api } from '../../../../api/client';

export type DeliveryBatchStatus = 'DRAFT' | 'READY' | 'ASSIGNED' | 'DISPATCHED' | 'COMPLETED' | 'CANCELLED';
export type DeliveryBatchOrderStatus = 'ACTIVE' | 'REMOVED' | 'COMPLETED';

export interface DeliveryBatch {
  id: string;
  tenantId: string;
  batchCode: string;
  deliveryZoneId: string;
  deliverySlotId?: string;
  riderId?: string;
  status: DeliveryBatchStatus;
  maxBatchSize: number;
  activeOrderCount: number;
  totalOrderCount: number;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

export interface DeliveryBatchOrder {
  id: string;
  tenantId: string;
  batchId: string;
  deliveryOrderId: string;
  sequenceHint?: number;
  status: DeliveryBatchOrderStatus;
  addedAt: string;
  addedBy: string;
  removedAt?: string;
  removedBy?: string;
  version: number;
}

export interface CreateDeliveryBatchPayload {
  deliveryZoneId: string;
  deliverySlotId?: string;
  maxBatchSize: number;
  deliveryOrderIds?: string[];
  riderId?: string;
}

export interface AutoClusterBatchesPayload {
  deliveryZoneId: string;
  deliverySlotId?: string;
  maxBatchSize?: number;
  maxDistanceKm?: number;
}

export interface AssignRiderToBatchPayload {
  riderId: string;
  override?: boolean;
  overrideReason?: string;
}

export interface AddOrdersToBatchPayload {
  deliveryOrderIds: string[];
}

export interface UpdateDeliveryBatchPayload {
  deliverySlotId?: string;
  maxBatchSize?: number;
}

export interface DeliveryBatchFilterParams {
  zoneId?: string;
  slotId?: string;
  riderId?: string;
  status?: DeliveryBatchStatus;
  page?: number;
  size?: number;
}

export const deliveryBatchApi = {
  getBatches: async (params?: DeliveryBatchFilterParams) => {
    const res = await api.get<{
      content: DeliveryBatch[];
      page: number;
      size: number;
      totalElements: number;
      totalPages: number;
    }>('/api/v1/deliveries/batches', { params });
    return res.data;
  },

  getBatch: async (batchId: string) => {
    const res = await api.get<DeliveryBatch>(`/api/v1/deliveries/batches/${batchId}`);
    return res.data;
  },

  getBatchOrders: async (batchId: string) => {
    const res = await api.get<DeliveryBatchOrder[]>(`/api/v1/deliveries/batches/${batchId}/orders`);
    return res.data;
  },

  createBatch: async (payload: CreateDeliveryBatchPayload) => {
    const res = await api.post<DeliveryBatch>('/api/v1/deliveries/batches', payload);
    return res.data;
  },

  autoCluster: async (payload: AutoClusterBatchesPayload) => {
    const res = await api.post<DeliveryBatch[]>('/api/v1/deliveries/batches/auto-cluster', payload);
    return res.data;
  },

  updateBatch: async (batchId: string, payload: UpdateDeliveryBatchPayload) => {
    const res = await api.put<DeliveryBatch>(`/api/v1/deliveries/batches/${batchId}`, payload);
    return res.data;
  },

  addOrders: async (batchId: string, payload: AddOrdersToBatchPayload) => {
    const res = await api.post<DeliveryBatch>(`/api/v1/deliveries/batches/${batchId}/orders`, payload);
    return res.data;
  },

  removeOrder: async (batchId: string, deliveryOrderId: string) => {
    const res = await api.delete<DeliveryBatch>(`/api/v1/deliveries/batches/${batchId}/orders/${deliveryOrderId}`);
    return res.data;
  },

  markReady: async (batchId: string) => {
    const res = await api.post<DeliveryBatch>(`/api/v1/deliveries/batches/${batchId}/ready`);
    return res.data;
  },

  assignRider: async (batchId: string, payload: AssignRiderToBatchPayload) => {
    const res = await api.post<DeliveryBatch>(`/api/v1/deliveries/batches/${batchId}/assign-rider`, payload);
    return res.data;
  },

  dispatchBatch: async (batchId: string) => {
    const res = await api.post<DeliveryBatch>(`/api/v1/deliveries/batches/${batchId}/dispatch`);
    return res.data;
  },

  completeBatch: async (batchId: string) => {
    const res = await api.post<DeliveryBatch>(`/api/v1/deliveries/batches/${batchId}/complete`);
    return res.data;
  },

  cancelBatch: async (batchId: string) => {
    const res = await api.post<DeliveryBatch>(`/api/v1/deliveries/batches/${batchId}/cancel`);
    return res.data;
  },
};
