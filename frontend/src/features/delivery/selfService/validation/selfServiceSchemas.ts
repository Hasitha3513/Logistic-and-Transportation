import { z } from 'zod';

export const preferenceSchema = z.object({
  emailEnabled: z.boolean(),
  smsEnabled: z.boolean(),
  version: z.number().nullable().optional(),
});

export const issueSchema = z.object({
  category: z.enum(['DELIVERY_TIMING', 'ACCESS_OR_ADDRESS_CLARIFICATION', 'DELIVERY_CONDITION', 'DELIVERY_SERVICE', 'OTHER']),
  description: z.string().trim().min(10).max(1000),
});

export const feedbackSchema = z.object({
  rating: z.number().int().min(1).max(5),
  comment: z.string().trim().max(1000),
});

export const deliveryRequestSchema = z.object({
  start: z.string().optional(),
  end: z.string().optional(),
  notes: z.string().trim().max(1000),
}).superRefine((value, context) => {
  if (Boolean(value.start) !== Boolean(value.end)) {
    context.addIssue({ code: 'custom', path: ['end'], message: 'Start and end are required together.' });
  }
  if (value.start && value.end && new Date(value.end).getTime() - new Date(value.start).getTime() < 1_800_000) {
    context.addIssue({ code: 'custom', path: ['end'], message: 'The requested window must be at least 30 minutes.' });
  }
});

export type PreferenceValues = z.infer<typeof preferenceSchema>;
export type IssueValues = z.infer<typeof issueSchema>;
export type FeedbackValues = z.infer<typeof feedbackSchema>;
export type DeliveryRequestValues = z.infer<typeof deliveryRequestSchema>;
