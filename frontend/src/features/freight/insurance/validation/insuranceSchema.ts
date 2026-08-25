import { z } from 'zod';

export const createPolicySchema = z.object({
  freightOrderId: z.string().uuid('Invalid freight order ID'),
  insuranceProvider: z.string().min(1, 'Insurance provider is required').max(160, 'Provider name too long'),
  policyType: z.string().min(1, 'Policy type is required').max(80, 'Policy type too long'),
  coverageAmount: z.number().positive('Coverage amount must be greater than 0'),
  premiumAmount: z.number().nonnegative('Premium amount must be >= 0'),
  deductibleAmount: z.number().nonnegative('Deductible amount must be >= 0'),
  currencyCode: z.string().length(3, 'Currency code must be 3 letters (e.g. USD)'),
  validFrom: z.string().min(1, 'Valid from date is required'),
  validUntil: z.string().min(1, 'Valid until date is required'),
  termsAndConditions: z.string().max(4000).optional()
}).refine(data => new Date(data.validUntil) > new Date(data.validFrom), {
  message: 'Valid until must be after valid from',
  path: ['validUntil']
});

export const updatePolicySchema = z.object({
  coverageAmount: z.number().positive('Coverage amount must be greater than 0'),
  premiumAmount: z.number().nonnegative('Premium amount must be >= 0'),
  deductibleAmount: z.number().nonnegative('Deductible amount must be >= 0'),
  validFrom: z.string().min(1, 'Valid from date is required'),
  validUntil: z.string().min(1, 'Valid until date is required'),
  status: z.enum(['ACTIVE', 'EXPIRED', 'CANCELLED']),
  termsAndConditions: z.string().max(4000).optional()
}).refine(data => new Date(data.validUntil) > new Date(data.validFrom), {
  message: 'Valid until must be after valid from',
  path: ['validUntil']
});

export const createClaimSchema = z.object({
  policyId: z.string().uuid('Invalid policy ID'),
  incidentDate: z.string().min(1, 'Incident date is required'),
  description: z.string().min(1, 'Description is required').max(2000),
  claimedAmount: z.number().positive('Claimed amount must be greater than 0'),
  currencyCode: z.string().length(3, 'Currency code must be 3 letters (e.g. USD)')
});

export const assessClaimSchema = z.object({
  assessedAmount: z.number().positive('Assessed amount must be greater than 0'),
  assessmentNotes: z.string().min(1, 'Assessment notes are required').max(2000)
});

export const reasonSchema = z.object({
  reason: z.string().min(1, 'Reason is required').max(1000)
});

export const recordSettlementSchema = z.object({
  amount: z.number().positive('Settlement amount must be greater than 0'),
  notes: z.string().max(1000).optional()
});

export type CreatePolicyFormData = z.infer<typeof createPolicySchema>;
export type UpdatePolicyFormData = z.infer<typeof updatePolicySchema>;
export type CreateClaimFormData = z.infer<typeof createClaimSchema>;
export type AssessClaimFormData = z.infer<typeof assessClaimSchema>;
export type ReasonFormData = z.infer<typeof reasonSchema>;
export type RecordSettlementFormData = z.infer<typeof recordSettlementSchema>;
