export interface LoadPlanItemPlacement {
  id: string;
  manifestItemId: string;
  placementOrder: number;
  zoneReference?: string | null;
  stackGroup?: string | null;
  containerReference?: string | null;
  loadingSequence: number;
  specialHandlingNotes?: string | null;
}

export type LoadPlanReadinessStatus = 'DRAFT' | 'STRUCTURALLY_READY';

export interface LoadPlan {
  id: string;
  loadPlanNumber: string;
  cargoManifestId: string;
  vehicleId: string;
  placements: LoadPlanItemPlacement[];
  notes?: string | null;
  readinessStatus: LoadPlanReadinessStatus;
  readyAt?: string | null;
  readyBy?: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

export interface LoadPlanItemPlacementPayload {
  manifestItemId: string;
  placementOrder: number;
  zoneReference?: string | null;
  stackGroup?: string | null;
  containerReference?: string | null;
  loadingSequence: number;
  specialHandlingNotes?: string | null;
}

export interface CreateLoadPlanPayload {
  cargoManifestId: string;
  vehicleId: string;
  placements?: LoadPlanItemPlacementPayload[];
  notes?: string | null;
}

export interface UpdateLoadPlanPayload {
  vehicleId: string;
  placements?: LoadPlanItemPlacementPayload[];
  notes?: string | null;
  version: number;
}

export interface MarkLoadPlanReadyPayload {
  version: number;
}

export interface LoadPlanViolation {
  code: string;
  message: string;
}

export interface LoadPlanValidationResponse {
  valid: boolean;
  violations: LoadPlanViolation[];
}

export interface LoadValidationResultResponse {
  loadPlanId: string;
  validatedAt: string;
  validatedBy: string;
  overallOutcome: 'PASS' | 'FAIL' | 'INCOMPLETE';
  grossWeightKg?: number | null;
  netWeightKg?: number | null;
  cubicVolumeM3?: number | null;
  payloadResult?: 'PASS' | 'FAIL' | 'INCOMPLETE' | null;
  volumeResult?: 'PASS' | 'FAIL' | 'INCOMPLETE' | null;
  axleResult?: 'PASS' | 'FAIL' | 'INCOMPLETE' | null;
  violations: { code: string; message: string }[];
  missingData: string[];
}
