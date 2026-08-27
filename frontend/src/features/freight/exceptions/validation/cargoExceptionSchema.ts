import { z } from 'zod';

export const createCargoExceptionSchema = z.object({
  exceptionType: z.enum([
    'DAMAGE',
    'PARTIAL_SHIPMENT',
    'WEIGHT_DISCREPANCY',
    'HAZARDOUS_MATERIAL',
    'UNMANIFESTED_CARGO',
    'SEAL_TAMPERING',
  ]),
  severity: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']).default('MEDIUM'),
  freightOrderId: z.string().uuid('Invalid freight order ID'),
  manifestId: z.string().uuid('Invalid manifest ID').optional().or(z.literal('')),
  manifestItemId: z.string().uuid('Invalid manifest item ID').optional().or(z.literal('')),
  description: z.string().min(1, 'Description is required').max(2000, 'Description too long'),
  impact: z.string().max(2000).optional(),
  restriction: z.string().max(1000).optional(),
  correctiveAction: z.string().max(2000).optional(),
});

export const holdExceptionSchema = z.object({
  restriction: z.string().max(1000).optional(),
  reason: z.string().max(2000).optional(),
});

export const escalateExceptionSchema = z.object({
  reason: z.string().min(1, 'Escalation reason is required').max(2000),
});

export const releaseExceptionSchema = z.object({
  reason: z.string().min(1, 'Release reason is required').max(2000),
});

export const rejectExceptionSchema = z.object({
  reason: z.string().min(1, 'Rejection reason is required').max(2000),
});

export const resolveExceptionSchema = z.object({
  resolution: z.string().min(1, 'Resolution description is required').max(2000),
  correctiveAction: z.string().max(2000).optional(),
  reason: z.string().max(2000).optional(),
});

export type CreateCargoExceptionFormData = z.infer<typeof createCargoExceptionSchema>;
export type HoldExceptionFormData = z.infer<typeof holdExceptionSchema>;
export type EscalateExceptionFormData = z.infer<typeof escalateExceptionSchema>;
export type ReleaseExceptionFormData = z.infer<typeof releaseExceptionSchema>;
export type RejectExceptionFormData = z.infer<typeof rejectExceptionSchema>;
export type ResolveExceptionFormData = z.infer<typeof resolveExceptionSchema>;
