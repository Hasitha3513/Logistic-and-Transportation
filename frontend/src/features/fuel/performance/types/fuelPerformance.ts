export type MeasurementMode = 'DISTANCE' | 'ENGINE_HOURS';
export type DataQuality = 'COMPLETE' | 'PARTIAL' | 'INSUFFICIENT' | 'INVALID_SOURCE_DATA';
export type Indicator = 'EFFICIENCY_DEVIATION' | 'POSSIBLE_LEAKAGE_INDICATOR' | 'REVIEW_REQUIRED';

export interface FuelPerformanceFilters {
  preset?: 7 | 30 | 90;
  from?: string;
  to?: string;
  vehicleId?: string;
  driverId?: string;
  vehicleTypeId?: string;
  fuelType?: string;
  measurementMode: MeasurementMode;
}

export interface Period { from: string; to: string; timeZone: string }
export interface Metrics {
  consumedLitres: number | null;
  distanceKm: number | null;
  engineHours: number | null;
  litresPer100Km: number | null;
  kmPerLitre: number | null;
  litresPerEngineHour: number | null;
  totalCost: number | null;
  consumptionRate: number | null;
  adverseVariancePercent: number | null;
  sampleCount: number;
  excludedQuantity: number;
  quality: DataQuality;
  exclusionReasons: Record<string, number>;
  indicators: Indicator[];
  currency: string | null;
  baseline: { type: string; period: Period | null; sampleCount: number; rate: number | null };
}
export interface Summary {
  period: Period;
  measurementMode: MeasurementMode;
  metrics: Metrics;
  vehicleCount: number;
  driverCount: number;
  calculatedAt: string;
}
export interface VehiclePerformance {
  vehicleId: string; vehicleLabel: string; vehicleTypeId: string; fuelType: string;
  measurementMode: MeasurementMode; metrics: Metrics; peerRate: number | null; calculatedAt: string;
}
export interface DriverPerformance {
  driverId: string; driverLabel: string; fuelType: string;
  measurementMode: MeasurementMode; metrics: Metrics; calculatedAt: string;
}
export interface Trend {
  bucketStart: string; bucketEnd: string; grain: string; actualRate: number | null;
  baselineRate: number | null; percentChange: number | null; quality: DataQuality; indicators: Indicator[];
}
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
