// Cargo Exception types (US-30 authoritative)

export type ExceptionType =
  | 'DAMAGE'
  | 'PARTIAL_SHIPMENT'
  | 'WEIGHT_DISCREPANCY'
  | 'HAZARDOUS_MATERIAL'
  | 'UNMANIFESTED_CARGO'
  | 'SEAL_TAMPERING';

export type ExceptionStatus = 'OPEN' | 'HELD' | 'ESCALATED' | 'RESOLVED' | 'REJECTED';

export type ExceptionSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface CargoExceptionHistoryEntry {
  id: string;
  action: string;
  actor: string;
  occurredAt: string;
  reason: string | null;
  details: string | null;
}

export interface CargoException {
  id: string;
  exceptionNumber: string;
  exceptionType: ExceptionType;
  status: ExceptionStatus;
  severity: ExceptionSeverity;
  freightOrderId: string;
  manifestId: string | null;
  manifestItemId: string | null;
  description: string;
  impact: string | null;
  restriction: string | null;
  correctiveAction: string | null;
  resolution: string | null;
  resolvedAt: string | null;
  resolvedBy: string | null;
  history: CargoExceptionHistoryEntry[];
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  version: number;
}

export interface CreateCargoExceptionPayload {
  exceptionType: ExceptionType;
  severity?: ExceptionSeverity;
  freightOrderId: string;
  manifestId?: string;
  manifestItemId?: string;
  description: string;
  impact?: string;
  restriction?: string;
  correctiveAction?: string;
}

export interface HoldExceptionPayload {
  restriction?: string;
  reason?: string;
  version: number;
}

export interface EscalateExceptionPayload {
  reason: string;
  version: number;
}

export interface ReleaseExceptionPayload {
  reason: string;
  version: number;
}

export interface RejectExceptionPayload {
  reason: string;
  version: number;
}

export interface ResolveExceptionPayload {
  resolution: string;
  correctiveAction?: string;
  reason?: string;
  version: number;
}

export interface CargoExceptionFilter {
  freightOrderId?: string;
  manifestId?: string;
  type?: ExceptionType;
  status?: ExceptionStatus;
  page?: number;
  size?: number;
}
