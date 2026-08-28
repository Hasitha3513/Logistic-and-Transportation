export interface FreightReportFilter {
  fromDate: string;
  toDate: string;
  customerId?: string;
  freightOrderId?: string;
  loadPlanStatus?: string;
  exceptionStatus?: string;
  policyStatus?: string;
  claimStatus?: string;
}

export interface FreightSummary {
  freightOrders: number;
  manifests: number;
  manifestItems: number;
  loadPlans: number;
  loadPlansByStatus: Record<string, number>;
  complianceOutcomes: Record<string, number>;
  policies: number;
  policiesByStatus: Record<string, number>;
  claims: number;
  claimsByStatus: Record<string, number>;
  settlements: number;
  cargoExceptions: number;
  exceptionsByStatus: Record<string, number>;
  exceptionsByType: Record<string, number>;
  unresolvedExceptions: number;
}

export interface FreightShipment {
  freightOrderId: string;
  orderNumber: string;
  customerId: string;
  manifestNumber?: string;
  loadPlanNumber?: string;
  loadPlanStatus?: string;
  cargoWeightKg?: number;
  cargoVolumeM3?: number;
  payloadUtilizationPercent?: number;
  volumeUtilizationPercent?: number;
  complianceOutcome: 'PASS' | 'FAIL' | 'INCOMPLETE';
  incompleteDiagnostics: string[];
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
