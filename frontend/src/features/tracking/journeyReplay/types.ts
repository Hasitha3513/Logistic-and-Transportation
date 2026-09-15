export type ReplaySelector = { vehicleId: string; tripId?: never } | { tripId: string; vehicleId?: never };
export type ReplayRange = { from?: string; to?: string };
export type ReplayQuery = ReplaySelector & ReplayRange & { limit?: number; cursor?: string };
export type ReplayCoverage = 'COMPLETE' | 'PARTIAL_RETENTION' | 'NO_DATA';
export type ReplayCoordinate = { latitude: number; longitude: number };
export type ReplayGap = { from: string; to: string; reasons: string[] };
export type ReplayAttribution = { tripId?: string; routeId?: string; routeVersion?: string; status: string };
export type ReplayPoint = {
  historyId: string; vehicleId: string; sourceTimestamp: string; receivedAt: string;
  coordinate: ReplayCoordinate; speedKph?: number; accuracyMeters?: number; trust: string;
  quality: string; ordering: string; attribution?: ReplayAttribution; qualityFlags: string[];
};
export type ReplayStop = {
  stopId: string; centroid: ReplayCoordinate; start: string; end: string; durationSeconds: number;
  pointCount: number; radiusMeters: number; startTruncated: boolean; endTruncated: boolean;
  quality: string; qualityFlags: string[];
};
export type ReplayPage<T> = {
  items: T[]; nextCursor?: string; snapshotRecordedAt: string;
  requestedRange: { from: string; to: string }; availableRange?: { from: string; to: string };
  coverage: ReplayCoverage; missingIntervals: ReplayGap[];
};
export type ReplayPointsPage = ReplayPage<ReplayPoint> & {
  truncated: boolean; browserCeilingWarning: boolean; unsupportedEvidence: string[];
};
export type ReplayStopsPage = ReplayPage<ReplayStop> & { analyzedPointCount: number; pointCeiling: number };
export type ReplayForm = { selectorType: 'VEHICLE' | 'TRIP'; selectorId: string; from: string; to: string };
