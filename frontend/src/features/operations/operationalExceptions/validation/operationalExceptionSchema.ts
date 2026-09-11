import { z } from 'zod';

export const operationalCommandSchema = z.object({
  note: z.string().trim().max(2000),
  category: z.enum(['OPERATIONAL', 'SAFETY', 'COMPLIANCE', 'CUSTOMER', 'FINANCIAL', 'TECHNICAL', 'SECURITY']),
  severity: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']),
  roleCode: z.string().trim().max(80),
});

export type OperationalCommandValues = z.infer<typeof operationalCommandSchema>;
