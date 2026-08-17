export type VehicleReadingType = 'ODOMETER' | 'ENGINE_HOURS';

export type VehicleReadingSourceType =
  | 'MANUAL'
  | 'TRIP_START'
  | 'TRIP_END'
  | 'FUEL_ISSUE'
  | 'MAINTENANCE'
  | 'TELEMATICS'
  | 'BASELINE'
  | 'METER_RESET';

export type CoverageStatus = 'COMPLETE' | 'PARTIAL' | 'NO_DATA';

export interface VehicleReading {
  id: string;
  vehicleId: string;
  readingType: VehicleReadingType;
  value: number;
  unit: string;
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
}

export interface ReadingSnapshot {
  readingId: string;
  value: number;
  unit: string;
  meterEpoch: number;
  sourceType: VehicleReadingSourceType;
  sourceReferenceId?: string | null;
  recordedAt: string;
  receivedAt: string;
}

export interface LatestVehicleReadings {
  vehicleId: string;
  odometer?: ReadingSnapshot | null;
  engineHours?: ReadingSnapshot | null;
}

export interface VehicleMeterReset {
  id: string;
  vehicleId: string;
  readingType: VehicleReadingType;
  fromEpoch: number;
  toEpoch: number;
  lastReadingValue: number;
  newMeterValue: number;
  effectiveAt: string;
  reason: string;
  createdBy: string;
  createdAt: string;
}

export interface VehicleMileageSummary {
  vehicleId: string;
  from?: string | null;
  to?: string | null;
  openingOdometer?: number | null;
  closingOdometer?: number | null;
  distanceTravelledKm?: number | null;
  openingEngineHours?: number | null;
  closingEngineHours?: number | null;
  engineHoursUsed?: number | null;
  meterResetCount: number;
  coverageStatus: CoverageStatus;
  abnormalDetected: boolean;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  limit: number;
  totalElements: number;
  totalPages: number;
}

export interface RecordManualReadingRequest {
  readingType: VehicleReadingType;
  value: number;
  recordedAt: string;
  idempotencyKey?: string;
  notes?: string;
}

export interface RecordCorrectionRequest {
  value: number;
  reason: string;
  recordedAt?: string;
}

export interface RecordMeterResetRequest {
  readingType: VehicleReadingType;
  newMeterValue: number;
  effectiveAt: string;
  reason: string;
}