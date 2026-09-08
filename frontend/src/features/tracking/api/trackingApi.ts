import { api } from '../../../api/client';
import type { TrackingDevice, TrackingHistoryPage, TrackingState } from '../types';
export const trackingApi = {
  vehicles: async () => (await api.get<TrackingState[]>('/v1/tracking/vehicles', { params: { page: 0, size: 100 } })).data,
  devices: async () => (await api.get<TrackingDevice[]>('/v1/tracking/devices', { params: { page: 0, size: 100 } })).data,
  history: async (vehicleId:string, cursor?:string) => { const to=new Date();const from=new Date(to.getTime()-86_400_000);return (await api.get<TrackingHistoryPage>(`/v1/tracking/vehicles/${vehicleId}/positions`,{params:{from:from.toISOString(),to:to.toISOString(),cursor,limit:100}})).data; },
  createDevice: async (input:{externalDeviceReference:string;providerAlias:string;hardwareSerialReference?:string}) => (await api.post<TrackingDevice>('/v1/tracking/devices',input)).data,
  lifecycle: async (device:TrackingDevice,lifecycle:'activate'|'disable') => (await api.post<TrackingDevice>(`/v1/tracking/devices/${device.id}/${lifecycle}`,{version:device.version})).data,
  associate: async (deviceId:string,vehicleId:string) => (await api.post(`/v1/tracking/devices/${deviceId}/associations`,{vehicleId,effectiveFrom:new Date().toISOString()})).data,
};
