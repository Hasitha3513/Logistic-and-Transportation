import { api } from '../../../../api/client';

export type DeliverySlotType = 'STANDARD' | 'EXPRESS' | 'SAME_DAY' | 'PEAK_WINDOW';
export type DeliverySlotStatus = 'ACTIVE' | 'INACTIVE' | 'CLOSED';
export type DeliverySlotReservationStatus = 'ACTIVE' | 'RELEASED' | 'CANCELLED';

export interface DeliverySlot {
  id: string;
  tenantId: string;
  deliveryZoneId: string;
  slotDate: string;
  startTime: string;
  endTime: string;
  slotType: DeliverySlotType;
  maxCapacity: number;
  reservedCapacity: number;
  remainingCapacity: number;
  cutoffTime?: string;
  bufferMinutes: number;
  status: DeliverySlotStatus;
  version: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface DeliverySlotReservation {
  id: string;
  deliverySlotId: string;
  deliveryOrderId: string;
  status: DeliverySlotReservationStatus;
  reservedAt: string;
  reservedBy: string;
  releasedAt?: string;
  releasedBy?: string;
  override: boolean;
  overrideReason?: string;
  version: number;
}

export interface CreateDeliverySlotPayload {
  deliveryZoneId: string;
  slotDate: string;
  startTime: string;
  endTime: string;
  slotType: DeliverySlotType;
  maxCapacity: number;
  cutoffTime?: string;
  bufferMinutes?: number;
}

export interface UpdateDeliverySlotPayload {
  startTime: string;
  endTime: string;
  slotType?: DeliverySlotType;
  maxCapacity: number;
  cutoffTime?: string;
  bufferMinutes?: number;
  expectedVersion: number;
}

export interface AssignDeliverySlotPayload {
  deliveryOrderId: string;
  managerOverride?: boolean;
  overrideReason?: string;
}

export const deliverySlotApi = {
  list: async (params?: { zoneId?: string; date?: string; startDate?: string; endDate?: string }) => {
    const { data } = await api.get<DeliverySlot[]>('/v1/delivery-slots', { params });
    return data;
  },

  get: async (id: string) => {
    const { data } = await api.get<DeliverySlot>(`/v1/delivery-slots/${id}`);
    return data;
  },

  create: async (payload: CreateDeliverySlotPayload) => {
    const { data } = await api.post<DeliverySlot>('/v1/delivery-slots', payload);
    return data;
  },

  update: async (id: string, payload: UpdateDeliverySlotPayload) => {
    const { data } = await api.put<DeliverySlot>(`/v1/delivery-slots/${id}`, payload);
    return data;
  },

  activate: async (id: string) => {
    const { data } = await api.post<DeliverySlot>(`/v1/delivery-slots/${id}/activate`);
    return data;
  },

  deactivate: async (id: string) => {
    const { data } = await api.post<DeliverySlot>(`/v1/delivery-slots/${id}/deactivate`);
    return data;
  },

  close: async (id: string) => {
    const { data } = await api.post<DeliverySlot>(`/v1/delivery-slots/${id}/close`);
    return data;
  },

  assignOrder: async (slotId: string, payload: AssignDeliverySlotPayload) => {
    const { data } = await api.post<DeliverySlotReservation>(`/v1/delivery-slots/${slotId}/assignments`, payload);
    return data;
  },

  releaseOrder: async (slotId: string, orderId: string) => {
    const { data } = await api.delete<DeliverySlotReservation>(`/v1/delivery-slots/${slotId}/assignments/${orderId}`);
    return data;
  },

  listReservations: async (slotId: string) => {
    const { data } = await api.get<DeliverySlotReservation[]>(`/v1/delivery-slots/${slotId}/reservations`);
    return data;
  },
};
