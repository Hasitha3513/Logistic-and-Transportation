export type Freshness = 'LIVE'|'RECENT'|'STALE'|'UNKNOWN';
export type Connectivity = 'CONNECTED'|'DEGRADED'|'OFFLINE'|'UNKNOWN';
export type Motion = 'MOVING'|'STATIONARY'|'UNKNOWN';
export type IncidentType = 'GEOFENCE'|'SPEED'|'ROUTE_DEVIATION';
export type SourceStatus = 'AVAILABLE'|'DEGRADED'|'UNAVAILABLE';
export type ProducerStatus = 'ACCEPTED'|'FIELD_FIDELITY_PENDING'|'FIELD_ACCEPTANCE_PENDING'|'UNAVAILABLE';

export interface DashboardQuery {
  vehicleIds?: string[];
  freshness?: Freshness[];
  connectivity?: Connectivity[];
  motion?: Motion[];
  incidentTypes?: IncidentType[];
  includeHeatMap: boolean;
  includeIncidents: boolean;
  cursor?: string;
  pageSize: number;
}
export interface Observation { sourceTimestamp:string;receivedAt:string;trust:'TRUSTED'|'UNTRUSTED'|'UNKNOWN';latitude?:number;longitude?:number;accuracyMeters?:number;speedKph?:number }
export interface DashboardTrip { tripId:string;lifecycle:string;routeId?:string;routeVersion?:number }
export interface DashboardVehicle { vehicleId:string;freshness:Freshness;connectivity:Connectivity;motion:Motion;latestReceived?:Observation;latestTrusted?:Observation;activeTrip?:DashboardTrip;incidentCounts:Partial<Record<IncidentType,number>>;journeyReplayAvailable:boolean }
export interface DashboardIncident { evidenceId:string;type:IncidentType;vehicleId:string;tripId?:string;routeId?:string;routeVersion?:number;severity?:string;status:string;sourceTimestamp:string;producerStatus:ProducerStatus }
export interface HeatCell { latitude:number;longitude:number;count:number }
export interface DashboardSummary { matchingVehicleCount:number;freshnessCounts:Partial<Record<Freshness,number>>;connectivityCounts:Partial<Record<Connectivity,number>>;motionCounts:Partial<Record<Motion,number>> }
export interface TrackingDashboard { evaluatedAt:string;lastSuccessfulRefreshAt:string;sourceStatus:SourceStatus;incidentSourceStatuses:Partial<Record<IncidentType,SourceStatus>>;producerStatuses:Record<string,ProducerStatus>;summary:DashboardSummary;vehicles:DashboardVehicle[];incidents:DashboardIncident[];heatMapCells:HeatCell[];nextCursor?:string }
