import { z } from 'zod';

export const deliveryOrderSchema = z.object({
  customerId: z.string().min(1, 'Select a customer'), originLocationId: z.string().min(1, 'Select an origin'),
  destinationLocationId: z.string().min(1, 'Select a destination'),
  priority: z.enum(['LOW', 'NORMAL', 'HIGH', 'URGENT']), serviceType: z.enum(['STANDARD', 'EXPRESS', 'SAME_DAY', 'SCHEDULED']),
  windowStart: z.string().min(1, 'Select a window start'), windowEnd: z.string().min(1, 'Select a window end'),
  instructions: z.string().max(2000).optional(),
}).refine((value) => !value.windowStart || !value.windowEnd || new Date(value.windowEnd) >= new Date(value.windowStart), { path: ['windowEnd'], message: 'Window end must not precede its start' });
export type DeliveryOrderFormValues = z.infer<typeof deliveryOrderSchema>;
