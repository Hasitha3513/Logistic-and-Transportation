import { z } from 'zod';

const code = z.string().trim().min(1, 'Required').max(60).regex(/^[A-Za-z0-9][A-Za-z0-9_-]*$/, 'Use letters, numbers, hyphens or underscores');
export const freightOrderSchema = z.object({
  customerId: z.string().min(1, 'Customer is required'), originLocationId: z.string().min(1, 'Origin is required'),
  destinationLocationId: z.string().min(1, 'Destination is required'), requestedPickupAt: z.string().min(1, 'Pickup time is required'),
  requestedDeliveryAt: z.string().min(1, 'Delivery time is required'), serviceLevel: code,
  priority: code.max(40), specialHandlingInstructions: z.string().max(2000).optional(),
  lines: z.array(z.object({ id: z.string().optional(), description: z.string().trim().min(1, 'Description is required').max(500), quantity: z.number().positive('Quantity must be greater than zero') })).min(1, 'At least one shipment line is required'),
}).superRefine((value, context) => {
  if (value.originLocationId === value.destinationLocationId) context.addIssue({ code: 'custom', path: ['destinationLocationId'], message: 'Destination must differ from origin' });
  if (value.requestedPickupAt && value.requestedDeliveryAt && new Date(value.requestedDeliveryAt) < new Date(value.requestedPickupAt)) context.addIssue({ code: 'custom', path: ['requestedDeliveryAt'], message: 'Delivery must not precede pickup' });
});
export type FreightOrderFormValues = z.infer<typeof freightOrderSchema>;
