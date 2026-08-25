import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { insuranceApi } from '../api/insuranceApi';
import {
  AssessClaimPayload,
  ApproveClaimPayload,
  CreateClaimPayload,
  CreatePolicyPayload,
  DisputeClaimPayload,
  RecordSettlementPayload,
  RejectClaimPayload,
  UpdatePolicyPayload
} from '../types/insurance';

export const insuranceKeys = {
  all: ['insurance'] as const,
  policies: () => [...insuranceKeys.all, 'policies'] as const,
  policy: (id: string) => [...insuranceKeys.policies(), id] as const,
  policyByOrder: (orderId: string) => [...insuranceKeys.policies(), 'by-order', orderId] as const,
  claims: () => [...insuranceKeys.all, 'claims'] as const,
  claim: (id: string) => [...insuranceKeys.claims(), id] as const,
  claimsByPolicy: (policyId: string) => [...insuranceKeys.claims(), 'by-policy', policyId] as const,
};

export const usePolicies = () => {
  return useQuery({
    queryKey: insuranceKeys.policies(),
    queryFn: insuranceApi.getPolicies,
  });
};

export const usePolicy = (id: string) => {
  return useQuery({
    queryKey: insuranceKeys.policy(id),
    queryFn: () => insuranceApi.getPolicyById(id),
    enabled: Boolean(id),
  });
};

export const usePolicyByFreightOrder = (orderId: string) => {
  return useQuery({
    queryKey: insuranceKeys.policyByOrder(orderId),
    queryFn: () => insuranceApi.getPolicyByFreightOrderId(orderId),
    enabled: Boolean(orderId),
  });
};

export const useCreatePolicy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreatePolicyPayload) => insuranceApi.createPolicy(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: insuranceKeys.policies() });
    },
  });
};

export const useUpdatePolicy = (id: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdatePolicyPayload) => insuranceApi.updatePolicy(id, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: insuranceKeys.policies() });
      queryClient.setQueryData(insuranceKeys.policy(id), data);
    },
  });
};

export const useClaims = () => {
  return useQuery({
    queryKey: insuranceKeys.claims(),
    queryFn: insuranceApi.getClaims,
  });
};

export const useClaim = (id: string) => {
  return useQuery({
    queryKey: insuranceKeys.claim(id),
    queryFn: () => insuranceApi.getClaimById(id),
    enabled: Boolean(id),
  });
};

export const useClaimsByPolicy = (policyId: string) => {
  return useQuery({
    queryKey: insuranceKeys.claimsByPolicy(policyId),
    queryFn: () => insuranceApi.getClaimsByPolicyId(policyId),
    enabled: Boolean(policyId),
  });
};

export const useCreateClaim = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateClaimPayload) => insuranceApi.createClaim(payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: insuranceKeys.claims() });
      queryClient.invalidateQueries({ queryKey: insuranceKeys.claimsByPolicy(data.policyId) });
    },
  });
};

export const useAssessClaim = (id: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: AssessClaimPayload) => insuranceApi.assessClaim(id, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: insuranceKeys.claims() });
      queryClient.setQueryData(insuranceKeys.claim(id), data);
    },
  });
};

export const useApproveClaim = (id: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ApproveClaimPayload) => insuranceApi.approveClaim(id, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: insuranceKeys.claims() });
      queryClient.setQueryData(insuranceKeys.claim(id), data);
    },
  });
};

export const useRejectClaim = (id: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: RejectClaimPayload) => insuranceApi.rejectClaim(id, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: insuranceKeys.claims() });
      queryClient.setQueryData(insuranceKeys.claim(id), data);
    },
  });
};

export const useDisputeClaim = (id: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: DisputeClaimPayload) => insuranceApi.disputeClaim(id, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: insuranceKeys.claims() });
      queryClient.setQueryData(insuranceKeys.claim(id), data);
    },
  });
};

export const useRecordSettlement = (id: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: RecordSettlementPayload) => insuranceApi.recordSettlement(id, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: insuranceKeys.claims() });
      queryClient.setQueryData(insuranceKeys.claim(id), data);
    },
  });
};
