export type RouteDisruptionType = 'ROAD_CLOSURE' | 'ACCIDENT' | 'WEATHER' | 'RESTRICTION';
export type DisruptionSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type DisruptionStatus = 'ACTIVE' | 'RESOLVED';

export interface Route {
  id: string;
  code: string;
  name: string;
  originLocationId: string;
  destinationLocationId: string;
  plannedDistanceKm: number;
  estimatedDurationMinutes: number;
  active: boolean;
  stopLocationIds: string[];
}

export interface RouteRevision {
  id: string;
  routeId: string;
  revisionNumber: number;
  code: string;
  name: string;
  originLocationId: string;
  destinationLocationId: string;
  plannedDistanceKm: number;
  estimatedDurationMinutes: number;
  active: boolean;
  stopLocationIds: string[];
  changedAt: string;
  changedBy: string;
}

export interface RouteDisruption {
  id: string;
  routeId: string;
  disruptionType: RouteDisruptionType;
  severity: DisruptionSeverity;
  description: string;
  effectiveFrom: string;
  effectiveUntil?: string | null;
  detourRouteId?: string | null;
  status: DisruptionStatus;
  createdAt: string;
  createdBy: string;
  resolvedAt?: string | null;
  resolvedBy?: string | null;
}

export interface CreateDisruptionInput {
  disruptionType: RouteDisruptionType;
  severity: DisruptionSeverity;
  description: string;
  effectiveFrom: string;
  effectiveUntil?: string | null;
  detourRouteId?: string | null;
}

export interface RouteOptimizationResult {
  routeId: string;
  originalStopLocationIds: string[];
  optimizedStopLocationIds: string[];
  originalEstimatedDistanceKm: number;
  optimizedEstimatedDistanceKm: number;
  originalEstimatedDurationMinutes: number;
  optimizedEstimatedDurationMinutes: number;
  distanceSavedKm: number;
  durationSavedMinutes: number;
  percentageDistanceImprovement: number;
}

export interface ApplyOptimizationInput {
  optimizedStopLocationIds: string[];
}

export interface RoutePerformanceAnalytics {
  routeId: string;
  routeCode: string;
  routeName: string;
  totalTripCount: number;
  completedTripCount: number;
  plannedDistanceKm: number;
  averageActualDistanceKm?: number | null;
  distanceVarianceKm?: number | null;
  distanceVariancePercent?: number | null;
  plannedDurationMinutes: number;
  averageActualDurationMinutes?: number | null;
  durationVarianceMinutes?: number | null;
  durationVariancePercent?: number | null;
  onTimeTripCount: number;
  delayedTripCount: number;
  averageDelayMinutes?: number | null;
}
