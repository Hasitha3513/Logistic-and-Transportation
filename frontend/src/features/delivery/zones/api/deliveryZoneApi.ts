import { api } from '../../../../api/client';

export interface DeliveryZoneCoordinate {
  longitude: number;
  latitude: number;
}

export type DeliveryZoneType = 'URBAN_DENSE' | 'SUBURBAN' | 'RURAL' | 'SPECIAL_SECURITY';
export type DeliveryZoneStatus = 'ACTIVE' | 'INACTIVE';

export interface DeliveryZone {
  id: string;
  tenantId: string;
  zoneCode: string;
  zoneName: string;
  description?: string;
  zoneType: DeliveryZoneType;
  status: DeliveryZoneStatus;
  serviceable: boolean;
  dailyCapacity?: number;
  depotLocationId?: string;
  coordinates: DeliveryZoneCoordinate[];
  minLatitude: number;
  maxLatitude: number;
  minLongitude: number;
  maxLongitude: number;
  approximateArea: number;
  priority: number;
  version: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface CreateDeliveryZonePayload {
  zoneCode: string;
  zoneName: string;
  description?: string;
  zoneType: DeliveryZoneType;
  serviceable?: boolean;
  dailyCapacity?: number;
  depotLocationId?: string;
  coordinates: DeliveryZoneCoordinate[];
  priority?: number;
}

export interface UpdateDeliveryZonePayload {
  zoneName: string;
  description?: string;
  zoneType: DeliveryZoneType;
  serviceable?: boolean;
  dailyCapacity?: number;
  depotLocationId?: string;
  coordinates: DeliveryZoneCoordinate[];
  priority?: number;
  expectedVersion: number;
}

export const deliveryZoneApi = {
  list: async (status?: DeliveryZoneStatus, serviceable?: boolean) => {
    const { data } = await api.get<DeliveryZone[]>('/v1/delivery-zones', {
      params: { status, serviceable },
    });
    return data;
  },

  get: async (id: string) => {
    const { data } = await api.get<DeliveryZone>(`/v1/delivery-zones/${id}`);
    return data;
  },

  create: async (payload: CreateDeliveryZonePayload) => {
    const { data } = await api.post<DeliveryZone>('/v1/delivery-zones', payload);
    return data;
  },

  update: async (id: string, payload: UpdateDeliveryZonePayload) => {
    const { data } = await api.put<DeliveryZone>(`/v1/delivery-zones/${id}`, payload);
    return data;
  },

  activate: async (id: string) => {
    const { data } = await api.post<DeliveryZone>(`/v1/delivery-zones/${id}/activate`);
    return data;
  },

  deactivate: async (id: string) => {
    const { data } = await api.post<DeliveryZone>(`/v1/delivery-zones/${id}/deactivate`);
    return data;
  },

  resolve: async (params: { locationId?: string; longitude?: number; latitude?: number }) => {
    const { data } = await api.get<DeliveryZone>('/v1/delivery-zones/resolve', { params });
    return data;
  },
};
