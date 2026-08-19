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

export type MaintenanceStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface MaintenanceSchedule {
  id: string;
  vehicleId: string;
  maintenanceType: string;
  scheduledStart: string;
  scheduledEnd: string;
  status: MaintenanceStatus;
  description?: string | null;
  serviceProvider?: string | null;
  cost?: number | null;
  createdAt: string;
  updatedAt: string;
  createdBy?: string | null;
  updatedBy?: string | null;
}

export interface MaintenanceScheduleRequest {
  maintenanceType: string;
  scheduledStart: string;
  scheduledEnd: string;
  description?: string;
  serviceProvider?: string;
  cost?: number;
}

export interface MaintenanceSchedulePatchRequest {
  maintenanceType?: string;
  scheduledStart?: string;
  scheduledEnd?: string;
  status?: MaintenanceStatus;
  description?: string;
  serviceProvider?: string;
  cost?: number;
}

export type DriverExceptionType = 'LEAVE' | 'DISCIPLINARY_SUSPENSION' | 'MEDICAL_EMERGENCY' | 'OTHER';
export type DriverExceptionStatus = 'SCHEDULED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface DriverException {
  id: string;
  driverId: string;
  exceptionType: DriverExceptionType;
  startTime: string;
  endTime: string;
  status: DriverExceptionStatus;
  reason?: string | null;
  remarks?: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy?: string | null;
  updatedBy?: string | null;
}

export interface DriverExceptionRequest {
  exceptionType: DriverExceptionType;
  startTime: string;
  endTime: string;
  reason?: string;
  remarks?: string;
}

export interface DriverExceptionPatchRequest {
  exceptionType?: DriverExceptionType;
  startTime?: string;
  endTime?: string;
  status?: DriverExceptionStatus;
  reason?: string;
  remarks?: string;
}

export interface DriverExceptionActionRequest {
  remarks?: string;
}

export type DriverViolationType =
  | 'SPEEDING'
  | 'RED_LIGHT'
  | 'RECKLESS_DRIVING'
  | 'UNAUTHORIZED_STOP'
  | 'LOGBOOK_VIOLATION'
  | 'ACCIDENT_FAULT'
  | 'OVERLOADING'
  | 'OTHER';

export type ViolationSeverity = 'MINOR' | 'MODERATE' | 'MAJOR' | 'CRITICAL';
export type FinePaymentStatus = 'UNPAID' | 'PAID' | 'WAIVED' | 'DISPUTED';

export interface DriverViolation {
  id: string;
  driverId: string;
  tripId?: string | null;
  violationType: DriverViolationType;
  severity: ViolationSeverity;
  violationDate: string;
  penaltyPoints: number;
  fineAmount: number;
  paymentStatus: FinePaymentStatus;
  paidAt?: string | null;
  paymentReference?: string | null;
  location?: string | null;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy?: string | null;
  updatedBy?: string | null;
}

export interface DriverViolationRequest {
  tripId?: string;
  violationType: DriverViolationType;
  severity: ViolationSeverity;
  violationDate: string;
  penaltyPoints?: number;
  fineAmount?: number;
  location?: string;
  description?: string;
}

export interface PayFineRequest {
  paidAt?: string;
  paymentReference?: string;
}

export interface WaiveFineRequest {
  reason: string;
}

export type PerformanceRating =
  | 'EXCELLENT'
  | 'GOOD'
  | 'SATISFACTORY'
  | 'NEEDS_IMPROVEMENT'
  | 'AT_RISK';

export interface DriverPerformanceSummary {
  driverId: string;
  driverName: string;
  totalTripsAssigned: number;
  totalTripsCompleted: number;
  totalTripsCancelled: number;
  tripCompletionRate: number;
  totalViolations: number;
  totalPenaltyPoints: number;
  criticalViolations: number;
  totalFines: number;
  unpaidFines: number;
  safetyScore: number;
  overallRating: PerformanceRating;
  evaluatedAt: string;
}

export type DriverMedicalStatus =
  | 'FIT'
  | 'FIT_WITH_RESTRICTIONS'
  | 'TEMPORARILY_UNFIT'
  | 'UNFIT';

export type VisionTestStatus =
  | 'PASSED'
  | 'PASSED_WITH_CORRECTIVE_LENSES'
  | 'FAILED'
  | 'NOT_TESTED';

export interface DriverMedicalRecord {
  id: string;
  driverId: string;
  assessmentDate: string;
  validFrom: string;
  validUntil: string;
  fitnessStatus: DriverMedicalStatus;
  visionTestStatus?: VisionTestStatus | null;
  restrictions?: string | null;
  examinerOrProvider?: string | null;
  certificateReference?: string | null;
  remarks?: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy?: string | null;
  updatedBy?: string | null;
}

export interface DriverMedicalRecordRequest {
  assessmentDate: string;
  validFrom: string;
  validUntil: string;
  fitnessStatus: DriverMedicalStatus;
  visionTestStatus?: VisionTestStatus;
  restrictions?: string;
  examinerOrProvider?: string;
  certificateReference?: string;
  remarks?: string;
}

export type DrugTestType =
  | 'RANDOM'
  | 'SCHEDULED'
  | 'PRE_EMPLOYMENT'
  | 'POST_INCIDENT'
  | 'REASONABLE_SUSPICION'
  | 'RETURN_TO_DUTY';

export type DrugTestResult = 'PENDING' | 'NEGATIVE' | 'POSITIVE' | 'INCONCLUSIVE';
export type DrugTestStatus = 'SCHEDULED' | 'SAMPLE_COLLECTED' | 'COMPLETED' | 'CANCELLED';

export interface DriverDrugTest {
  id: string;
  driverId: string;
  testType: DrugTestType;
  scheduledDate: string;
  sampleCollectedAt?: string | null;
  resultDate?: string | null;
  result: DrugTestResult;
  status: DrugTestStatus;
  laboratoryOrProvider?: string | null;
  referenceNumber?: string | null;
  remarks?: string | null;
  returnToDutyRequired: boolean;
  returnToDutyClearedAt?: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy?: string | null;
  updatedBy?: string | null;
}

export interface DriverDrugTestRequest {
  testType: DrugTestType;
  scheduledDate: string;
  laboratoryOrProvider?: string;
  referenceNumber?: string;
  remarks?: string;
}

export interface DriverDrugTestResultRequest {
  result: DrugTestResult;
  resultDate?: string;
  remarks?: string;
  returnToDutyRequired?: boolean;
}