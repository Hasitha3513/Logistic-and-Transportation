export type Freshness = 'LIVE' | 'RECENT' | 'STALE' | 'UNKNOWN';
export type Connectivity = 'CONNECTED' | 'DEGRADED' | 'OFFLINE' | 'UNKNOWN';
export type Trust = 'TRUSTED' | 'UNTRUSTED' | 'UNKNOWN';
export interface Position { id:string; vehicleId:string; sourceTimestamp:string; receivedAt:string; latitude:number; longitude:number; accuracyMeters?:number; horizontalAccuracyMeters?:number; trust:Trust; quality:string; ordering:string }
export interface TrackingState { vehicleId:string; latestReceived?:Position; latestTrusted?:Position; freshness:Freshness; connectivity:Connectivity; policyVersion:string; evaluatedAt:string }
export interface TrackingDevice { id:string; externalReference:string; providerAlias:string; hardwareSerialReference?:string; lifecycle:'ACTIVE'|'DISABLED'; lastSeenAt?:string; version:number }
export interface TrackingHistoryPage { items: Position[]; nextCursor?: string }
