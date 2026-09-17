export type GpsExceptionStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RECOVERING' | 'RESOLVED';
export type GpsExceptionSeverity = 'WARNING' | 'HIGH';
export type GpsExceptionType = 'INVALID_TELEMETRY' | 'CLOCK_ANOMALY' | 'LOW_ACCURACY' | 'IMPOSSIBLE_MOVEMENT' | 'SIGNAL_LOSS' | 'DEVICE_TAMPER' | 'BATTERY_LOW' | 'BATTERY_RAPID_DRAIN' | 'BINDING_VIOLATION' | 'PROCESSING_FAILURE';

export interface GpsExceptionEpisode {
  id: string;
  deviceId: string;
  vehicleId: string;
  type: GpsExceptionType;
  severity: GpsExceptionSeverity;
  status: GpsExceptionStatus;
  openedAt: string;
  lastObservedAt: string;
  resolvedAt?: string | null;
  evidenceCount: number;
  consecutiveRecoveryPoints: number;
  version: number;
}

export interface GpsExceptionEvidence {
  id: string;
  sourceTimestamp: string;
  assessedAt: string;
  trust: 'TRUSTED' | 'UNTRUSTED' | 'UNKNOWN';
  ordering: 'IN_ORDER' | 'OUT_OF_ORDER' | 'EQUAL_SOURCE_TIME';
  reliabilityState: 'NORMAL' | 'DEGRADED' | 'SUSPECT' | 'OFFLINE' | 'RECOVERING' | 'UNKNOWN';
  qualityCodes: string[];
  transition: string;
}

export interface CursorPage<T> { items: T[]; nextCursor?: string | null }
export interface EpisodeFilters {
  from: string;
  to: string;
  status?: GpsExceptionStatus;
  type?: GpsExceptionType;
  severity?: GpsExceptionSeverity;
  vehicleId?: string;
  deviceId?: string;
  cursor?: string;
  limit?: number;
}
export interface AcknowledgementCommand { expectedVersion: number; reason: string; idempotencyKey: string }
export interface Acknowledgement { episodeId: string; status: string; severity: string; version: number; acknowledgedAt: string }
