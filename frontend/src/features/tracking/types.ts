export type Freshness = 'LIVE' | 'RECENT' | 'STALE' | 'UNKNOWN';
export type Connectivity = 'CONNECTED' | 'DEGRADED' | 'OFFLINE' | 'UNKNOWN';
export type Trust = 'TRUSTED' | 'UNTRUSTED' | 'UNKNOWN';
export interface Position { id:string; vehicleId:string; sourceTimestamp:string; receivedAt:string; latitude:number; longitude:number; accuracyMeters?:number; horizontalAccuracyMeters?:number; trust:Trust; quality:string; ordering:string }
export interface TrackingState { vehicleId:string; latestReceived?:Position; latestTrusted?:Position; freshness:Freshness; connectivity:Connectivity; policyVersion:string; evaluatedAt:string }
export type TrackingDeviceLifecycle = 'DRAFT' | 'ACTIVE' | 'DISABLED' | 'RETIRED';
export interface CurrentProviderBinding {
  bindingId:string; providerConnectionId:string; bindingLifecycle:ProviderConnectionLifecycle;
  bindingVersion:number; providerDisplayName:string; providerType:string; providerAlias:string;
  connectionLifecycle:ProviderConnectionLifecycle; maskedExternalDeviceReference:string;
  safeConfiguration:Record<string,string>;
}
export interface CurrentVehicleAssociation { associationId:string; vehicleId:string; effectiveFrom:string }
export interface TrackingDevice {
  id:string; externalReference:string; providerAlias:string; hardwareSerialReference?:string;
  lifecycle:TrackingDeviceLifecycle; registeredAt?:string; lastSeenAt?:string; version:number;
  currentProviderBinding?:CurrentProviderBinding|null;
  currentVehicleAssociation?:CurrentVehicleAssociation|null;
}
export interface TrackingHistoryPage { items: Position[]; nextCursor?: string }

export type ProviderConnectionLifecycle = 'DRAFT' | 'ACTIVE' | 'DISABLED' | 'RETIRED';
export type ProviderConnectionTestStatus = 'NOT_TESTED' | 'PASS' | 'AUTH_FAILED' | 'UNREACHABLE' | 'INVALID_CONFIGURATION';
export interface TrackingProviderType { providerType:string; capabilities:string[]; supported:boolean }
export interface TrackingProviderConnection {
  id:string; providerType:string; displayName:string; providerAlias:string; endpointUri?:string;
  safeConfiguration:Record<string,string>; credentialConfigured:boolean; pollIntervalSeconds:number;
  pageSize:number; lifecycle:ProviderConnectionLifecycle; testStatus:ProviderConnectionTestStatus;
  lastTestedAt?:string; lastSuccessfulPollAt?:string; lastProviderMessageAt?:string;
  lastErrorCategory?:string; nextPollAt?:string; version:number;
}
export interface TrackingProviderConnectionPage { items:TrackingProviderConnection[]; page:number; size:number; total:number }
export interface ProviderConnectionInput {
  providerType:string; displayName:string; providerAlias:string; providerKeyId:string;
  endpointUri?:string; safeConfiguration:Record<string,string>; credentialReference:string;
  pollIntervalSeconds:number; pageSize:number;
}
export interface ProviderConnectionUpdateInput {
  displayName:string; endpointUri?:string; safeConfiguration:Record<string,string>;
  credentialReference?:string; pollIntervalSeconds:number; pageSize:number; version:number;
}
export interface ProviderConnectionTestResult { connection:TrackingProviderConnection; status:ProviderConnectionTestStatus; detailCode?:string }
export interface DiscoveredTrackingDevice { maskedExternalDeviceReference:string; displayName?:string; capabilities:string[] }
export interface TrackingDiscoveryResult { status:string; devices:DiscoveredTrackingDevice[]; nextCursor?:string }
export interface DeviceProviderBindingInput {
  providerConnectionId:string; externalDeviceReference:string; safeConfiguration:Record<string,string>;
  lifecycle:'ACTIVE'; currentBindingVersion?:number;
}
