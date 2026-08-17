export type VehicleReadingType = 'ODOMETER' | 'ENGINE_HOURS';
export type VehicleReadingSourceType = 'MANUAL' | 'BASELINE' | 'TRIP_START' | 'TRIP_END' | 'FUEL_ISSUE' | 'METER_RESET' | 'TELEMATICS' | 'MAINTENANCE';
export type VehicleReadingUnit = 'KILOMETER' | 'HOUR';
export type ReadingStatus = 'ACTIVE' | 'CORRECTED' | 'CORRECTION';
export type CoverageStatus = 'COMPLETE' | 'PARTIAL' | 'NO_DATA';
export type TripDistanceStatus = 'AVAILABLE' | 'PARTIAL' | 'UNAVAILABLE';

export interface VehicleReadingResponse {
  id: string;
  vehicleId: string;
  readingType: VehicleReadingType;
  value: number;
  unit: VehicleReadingUnit;
  meterEpoch: number;
  sourceType: VehicleReadingSourceType;
  sourceReferenceId?: string | null;
  recordedAt: string;
  receivedAt: string;
  createdBy: string;
  correctionOfReadingId?: string | null;
  correctionReason?: string | null;
  idempotencyKey?: string | null;
  notes?: string | null;
  createdAt: string;
  status: ReadingStatus;
}

export interface LatestReadingsResponse {
  vehicleId: string;
  odometer: VehicleReadingResponse | null;
  engineHours: VehicleReadingResponse | null;
}

export interface ManualReadingRequest {
  readingType: VehicleReadingType;
  value: number;
  recordedAt: string;
  idempotencyKey?: string;
  notes?: string;
}

export interface CorrectionRequest {
  value: number;
  reason: string;
  idempotencyKey?: string;
  notes?: string;
}

export interface MeterResetRequest {
  readingType: VehicleReadingType;
  newMeterValue: number;
  effectiveAt: string;
  reason: string;
  notes?: string;
}

export interface MeterResetResponse {
  id: string;
  vehicleId: string;
  readingType: VehicleReadingType;
  previousReadingId?: string | null;
  previousMeterValue: number;
  newReadingId: string;
  newMeterValue: number;
  effectiveAt: string;
  reason: string;
  createdBy: string;
  approvedBy?: string | null;
  notes?: string | null;
  createdAt: string;
}

export interface VehicleMileageSummaryResponse {
  vehicleId: string;
  from: string;
  to: string;
  openingOdometer: number | null;
  closingOdometer: number | null;
  distanceKm: number;
  openingEngineHours: number | null;
  closingEngineHours: number | null;
  engineHoursUsed: number;
  readingCount: number;
  correctionCount: number;
  meterResetCount: number;
  sourceCounts: Partial<Record<VehicleReadingSourceType, number>>;
  coverageStatus: CoverageStatus;
  coverageReason?: string | null;
}

export interface TripDistanceSummaryResponse {
  tripId: string;
  vehicleId: string;
  startOdometer: number | null;
  endOdometer: number | null;
  distanceKm: number | null;
  status: TripDistanceStatus;
  meterResetEncountered: boolean;
  notes?: string | null;
}

export interface PageResult<T> {
  content: T[];
  page: number;
  limit: number;
  totalElements: number;
  totalPages: number;
}
