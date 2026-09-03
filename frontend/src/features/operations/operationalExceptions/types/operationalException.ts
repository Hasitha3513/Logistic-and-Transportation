export type ExceptionStatus = 'OPEN' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type ExceptionSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ExceptionCategory = 'OPERATIONAL' | 'SAFETY' | 'COMPLIANCE' | 'CUSTOMER' | 'FINANCIAL' | 'TECHNICAL' | 'SECURITY';

export interface OperationalExceptionCase {
  id: string;
  caseReference: string;
  sourceModule: 'ROUTING' | 'DELIVERY';
  sourceType: string;
  sourceId: string;
  occurredAt: string;
  summaryCode: string;
  category: ExceptionCategory;
  severity: ExceptionSeverity;
  status: ExceptionStatus;
  slaStatus: 'ON_TRACK' | 'AT_RISK' | 'BREACHED' | 'MET';
  responseDueAt: string;
  resolutionDueAt: string;
  nextEscalationAt?: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  closedAt?: string;
  assignmentType?: 'ROLE_QUEUE' | 'USER';
  assignedUserId?: string;
  assignedRoleCode?: string;
  escalationLevel: 'L0' | 'L1' | 'L2' | 'L3';
  resolutionNote?: string;
  resolutionResultReference?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CorrectiveAction {
  id: string;
  type: 'CORRECTIVE' | 'PREVENTIVE';
  description: string;
  ownerType: 'ROLE_QUEUE' | 'USER';
  ownerUserId?: string;
  ownerRoleCode?: string;
  dueAt?: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  completedAt?: string;
  evidenceReference?: string;
  version: number;
}

export interface RootCauseAnalysis {
  id: string;
  causeCategory: 'PEOPLE' | 'PROCESS' | 'EQUIPMENT' | 'EXTERNAL' | 'SYSTEM_DATA' | 'ENVIRONMENT' | 'UNKNOWN';
  rootCauseCode: string;
  summary: string;
  contributingFactors?: string;
  authorId: string;
  approverId?: string;
  approvedAt?: string;
  version: number;
}

export interface OperationalExceptionDetail {
  exceptionCase: OperationalExceptionCase;
  correctiveActions: CorrectiveAction[];
  rca?: RootCauseAnalysis;
}

export interface OperationalExceptionHistory {
  id: string;
  action: string;
  beforeValue?: string;
  afterValue?: string;
  reason?: string;
  actorUsername: string;
  occurredAt: string;
}

export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
