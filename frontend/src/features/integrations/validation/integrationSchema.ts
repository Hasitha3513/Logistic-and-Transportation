import { z } from 'zod';

export const integrationSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(160),
  endpointAlias: z.literal('CONTROLLED_SANDBOX'),
  credentialReference: z.string().trim().max(160).optional(),
});
export type IntegrationFormValues = z.infer<typeof integrationSchema>;
