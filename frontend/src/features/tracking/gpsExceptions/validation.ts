import { z } from 'zod';

export const acknowledgementSchema = z.object({ reason: z.string().trim().min(1, 'Reason is required').max(500, 'Reason must be 500 characters or fewer') });
export type AcknowledgementValues = z.infer<typeof acknowledgementSchema>;

export function validateRange(from: string, to: string): string | undefined {
  if (!from || !to) return 'Choose a required start and end time.';
  const start = Date.parse(from), end = Date.parse(to);
  if (!Number.isFinite(start) || !Number.isFinite(end) || start >= end) return 'Start time must be before end time.';
  if (end - start > 7 * 86_400_000) return 'The selected range cannot exceed seven days.';
  return undefined;
}
