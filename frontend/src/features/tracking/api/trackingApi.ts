import { api } from '../../../api/client';
import type { ProviderConnectionInput, ProviderConnectionTestResult, ProviderConnectionUpdateInput, TrackingDevice, TrackingDiscoveryResult, TrackingHistoryPage, TrackingProviderConnection, TrackingProviderConnectionPage, TrackingProviderType, TrackingState } from '../types';
export const trackingApi = {
  vehicles: async () => (await api.get<TrackingState[]>('/v1/tracking/vehicles', { params: { page: 0, size: 100 } })).data,
  devices: async () => (await api.get<TrackingDevice[]>('/v1/tracking/devices', { params: { page: 0, size: 100 } })).data,
  history: async (vehicleId:string, cursor?:string) => { const to=new Date();const from=new Date(to.getTime()-86_400_000);return (await api.get<TrackingHistoryPage>(`/v1/tracking/vehicles/${vehicleId}/positions`,{params:{from:from.toISOString(),to:to.toISOString(),cursor,limit:100}})).data; },
  createDevice: async (input:{externalDeviceReference:string;providerAlias:string;hardwareSerialReference?:string}) => (await api.post<TrackingDevice>('/v1/tracking/devices',input)).data,
  lifecycle: async (device:TrackingDevice,lifecycle:'activate'|'disable') => (await api.post<TrackingDevice>(`/v1/tracking/devices/${device.id}/${lifecycle}`,{version:device.version})).data,
  associate: async (deviceId:string,vehicleId:string) => (await api.post(`/v1/tracking/devices/${deviceId}/associations`,{vehicleId,effectiveFrom:new Date().toISOString()})).data,
  providerTypes: async () => (await api.get<TrackingProviderType[]>('/v1/tracking/provider-types')).data,
  providerConnections: async (page=0,size=20) => (await api.get<TrackingProviderConnectionPage>('/v1/tracking/provider-connections',{params:{page,size}})).data,
  providerConnection: async (id:string) => (await api.get<TrackingProviderConnection>(`/v1/tracking/provider-connections/${id}`)).data,
  createProviderConnection: async (input:ProviderConnectionInput) => (await api.post<TrackingProviderConnection>('/v1/tracking/provider-connections',input)).data,
  updateProviderConnection: async ({id,input}:{id:string;input:ProviderConnectionUpdateInput}) => (await api.put<TrackingProviderConnection>(`/v1/tracking/provider-connections/${id}`,input)).data,
  testProviderConnection: async (id:string) => (await api.post<ProviderConnectionTestResult>(`/v1/tracking/provider-connections/${id}/test`)).data,
  providerConnectionLifecycle: async ({connection,action}:{connection:TrackingProviderConnection;action:'activate'|'disable'|'retire'}) => (await api.post<TrackingProviderConnection>(`/v1/tracking/provider-connections/${connection.id}/${action}`,{version:connection.version})).data,
  discoverProviderDevices: async (id:string,cursor?:string) => (await api.get<TrackingDiscoveryResult>(`/v1/tracking/provider-connections/${id}/devices/discover`,{params:{limit:100,cursor}})).data,
};
