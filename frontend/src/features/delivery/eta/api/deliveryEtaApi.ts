import { api } from '../../../../api/client';

export type EtaStatus = 'ON_TIME' | 'AT_RISK' | 'LATE';
export type EtaSource = 'HEURISTIC' | 'HEURISTIC_FALLBACK' | 'EXTERNAL_PROVIDER';

export interface SingleOrderEta {
  orderId: string;
  estimatedArrivalAt: string;
  travelDurationSeconds: number;
  distanceMeters: number;
  slaStatus?: EtaStatus;
  source: EtaSource;
  calculatedAt: string;
  staleAt: string;
  isStale: boolean;
}

export interface BatchEtaStop {
  deliveryOrderId: string;
  sequence: number;
  estimatedArrivalAt: string;
  travelDurationSeconds: number;
  serviceDurationSeconds: number;
  distanceMeters: number;
  slaStatus?: EtaStatus;
}

export interface BatchEta {
  batchId: string;
  calculatedAt: string;
  staleAt: string;
  totalDurationSeconds: number;
  totalDistanceMeters: number;
  estimatedCompletionAt: string;
  source: EtaSource;
  isStale: boolean;
  stops: BatchEtaStop[];
}

export const deliveryEtaApi = {
  getOrderEta: async (orderId: string): Promise<SingleOrderEta> => {
    const res = await api.get<SingleOrderEta>(`/api/v1/deliveries/orders/${orderId}/eta`);
    return res.data;
  },

  calculateOrderEta: async (orderId: string): Promise<SingleOrderEta> => {
    const res = await api.post<SingleOrderEta>(`/api/v1/deliveries/orders/${orderId}/eta/calculate`);
    return res.data;
  },

  getBatchEta: async (batchId: string): Promise<BatchEta> => {
    const res = await api.get<BatchEta>(`/api/v1/deliveries/batches/${batchId}/eta`);
    return res.data;
  },

  calculateBatchEta: async (batchId: string): Promise<BatchEta> => {
    const res = await api.post<BatchEta>(`/api/v1/deliveries/batches/${batchId}/eta/calculate`);
    return res.data;
  }
};
