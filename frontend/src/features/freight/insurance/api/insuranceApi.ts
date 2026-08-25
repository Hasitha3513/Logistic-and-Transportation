import { api } from '../../../../api/client';
import {
  ClaimResponse,
  CreateClaimPayload,
  CreatePolicyPayload,
  PolicyResponse,
  UpdatePolicyPayload,
  AssessClaimPayload,
  ApproveClaimPayload,
  RejectClaimPayload,
  DisputeClaimPayload,
  RecordSettlementPayload
} from '../types/insurance';

const POLICY_BASE_URL = '/v1/freight/insurance/policies';
const CLAIM_BASE_URL = '/v1/freight/insurance/claims';

export const insuranceApi = {
  // Policies
  getPolicies: async (): Promise<PolicyResponse[]> => {
    const response = await api.get<PolicyResponse[]>(POLICY_BASE_URL);
    return response.data;
  },

  getPolicyById: async (id: string): Promise<PolicyResponse> => {
    const response = await api.get<PolicyResponse>(`${POLICY_BASE_URL}/${id}`);
    return response.data;
  },

  getPolicyByFreightOrderId: async (orderId: string): Promise<PolicyResponse> => {
    const response = await api.get<PolicyResponse>(`${POLICY_BASE_URL}/by-order/${orderId}`);
    return response.data;
  },

  createPolicy: async (payload: CreatePolicyPayload): Promise<PolicyResponse> => {
    const response = await api.post<PolicyResponse>(POLICY_BASE_URL, payload);
    return response.data;
  },

  updatePolicy: async (id: string, payload: UpdatePolicyPayload): Promise<PolicyResponse> => {
    const response = await api.put<PolicyResponse>(`${POLICY_BASE_URL}/${id}`, payload);
    return response.data;
  },

  // Claims
  getClaims: async (): Promise<ClaimResponse[]> => {
    const response = await api.get<ClaimResponse[]>(CLAIM_BASE_URL);
    return response.data;
  },

  getClaimById: async (id: string): Promise<ClaimResponse> => {
    const response = await api.get<ClaimResponse>(`${CLAIM_BASE_URL}/${id}`);
    return response.data;
  },

  getClaimsByPolicyId: async (policyId: string): Promise<ClaimResponse[]> => {
    const response = await api.get<ClaimResponse[]>(`${CLAIM_BASE_URL}/by-policy/${policyId}`);
    return response.data;
  },

  createClaim: async (payload: CreateClaimPayload): Promise<ClaimResponse> => {
    const response = await api.post<ClaimResponse>(CLAIM_BASE_URL, payload);
    return response.data;
  },

  assessClaim: async (id: string, payload: AssessClaimPayload): Promise<ClaimResponse> => {
    const response = await api.post<ClaimResponse>(`${CLAIM_BASE_URL}/${id}/assess`, payload);
    return response.data;
  },

  approveClaim: async (id: string, payload: ApproveClaimPayload): Promise<ClaimResponse> => {
    const response = await api.post<ClaimResponse>(`${CLAIM_BASE_URL}/${id}/approve`, payload);
    return response.data;
  },

  rejectClaim: async (id: string, payload: RejectClaimPayload): Promise<ClaimResponse> => {
    const response = await api.post<ClaimResponse>(`${CLAIM_BASE_URL}/${id}/reject`, payload);
    return response.data;
  },

  disputeClaim: async (id: string, payload: DisputeClaimPayload): Promise<ClaimResponse> => {
    const response = await api.post<ClaimResponse>(`${CLAIM_BASE_URL}/${id}/dispute`, payload);
    return response.data;
  },

  recordSettlement: async (id: string, payload: RecordSettlementPayload): Promise<ClaimResponse> => {
    const response = await api.post<ClaimResponse>(`${CLAIM_BASE_URL}/${id}/settlements`, payload);
    return response.data;
  }
};
