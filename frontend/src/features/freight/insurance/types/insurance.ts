export type PolicyStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED';

export type ClaimStatus = 'OPEN' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'DISPUTED' | 'SETTLED';

export interface PolicyResponse {
  id: string;
  policyNumber: string;
  freightOrderId: string;
  insuranceProvider: string;
  policyType: string;
  coverageAmount: number;
  premiumAmount: number;
  deductibleAmount?: number;
  currencyCode?: string;
  currency?: string;
  validFrom: string;
  validUntil: string;
  status: PolicyStatus;
  termsAndConditions?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

export interface ClaimSettlementResponse {
  id: string;
  settlementReference: string;
  settledAmount: number;
  currencyCode: string;
  settlementNotes?: string;
  settledBy: string;
  settledAt: string;
}

export interface ClaimResponse {
  id: string;
  claimNumber: string;
  policyId: string;
  incidentDate: string;
  description: string;
  claimedAmount: number;
  assessedAmount?: number;
  totalSettledAmount: number;
  currencyCode: string;
  status: ClaimStatus;
  assessmentNotes?: string;
  resolutionReason?: string;
  assessedBy?: string;
  assessedAt?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  settlements: ClaimSettlementResponse[];
}

export interface CreatePolicyPayload {
  freightOrderId: string;
  insuranceProvider: string;
  policyType: string;
  coverageAmount: number;
  premiumAmount: number;
  deductibleAmount: number;
  currencyCode: string;
  validFrom: string;
  validUntil: string;
  termsAndConditions?: string;
}

export interface UpdatePolicyPayload {
  coverageAmount: number;
  premiumAmount: number;
  deductibleAmount: number;
  validFrom: string;
  validUntil: string;
  status: PolicyStatus;
  termsAndConditions?: string;
  version: number;
}

export interface CreateClaimPayload {
  policyId: string;
  incidentDate: string;
  description: string;
  claimedAmount: number;
  currencyCode: string;
}

export interface AssessClaimPayload {
  assessedAmount: number;
  assessmentNotes: string;
  version: number;
}

export interface ApproveClaimPayload {
  notes?: string;
  version: number;
}

export interface RejectClaimPayload {
  reason: string;
  version: number;
}

export interface DisputeClaimPayload {
  reason: string;
  version: number;
}

export interface RecordSettlementPayload {
  amount: number;
  notes?: string;
  version: number;
}
