export type IdleState = 'UNSUPPORTED' | 'NOT_REPORTED' | 'STALE' | 'CONFLICTING_EVIDENCE' | 'CANDIDATE' | 'IDLE' | 'NORMAL';

export interface CursorPage<T> { items: T[]; nextCursor?: string | null }
export interface IdleStateView { vehicleId:string; vehicleLabel?:string|null; state:IdleState; capabilityState:string; latestSourceTimestamp?:string|null; candidateStartedAt?:string|null; lastQualifyingAt?:string|null; creditedSeconds:number; evidenceCount:number; version:number; fuelEstimateAvailability:string; fuelEstimateSource?:string|null }
export interface IdleEpisode { id:string; vehicleId:string; vehicleLabel?:string|null; lifecycle:string; startSourceTimestamp:string; confirmedAt:string; lastSourceTimestamp:string; endSourceTimestamp?:string|null; endReason?:string|null; confirmedDurationSeconds:number; evidenceCount:number; version:number; fuelEstimateAvailability:string; fuelEstimateSource?:string|null }
export interface IdleEvidence { id:string; sourceTimestamp:string; evidenceQuality:string; creditedDeltaSeconds:number; recordedAt:string }
export interface StateFilters { vehicleId?:string; state?:IdleState; cursor?:string; limit:number }
export interface EpisodeFilters { vehicleId?:string; from:string; to:string; endReason?:string; cursor?:string; limit:number }
