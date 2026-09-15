import { z } from 'zod';

export const replayFormSchema = z.object({
  selectorType: z.enum(['VEHICLE', 'TRIP']),
  selectorId: z.string().uuid('Enter a valid Vehicle or Trip UUID.'),
  from: z.string(),
  to: z.string(),
}).superRefine((value, context) => {
  if (!value.from && !value.to) return;
  if (!value.from || !value.to) {
    context.addIssue({ code: 'custom', path: [value.from ? 'to' : 'from'], message: 'Provide both range boundaries.' });
    return;
  }
  const from = Date.parse(value.from), to = Date.parse(value.to);
  if (!Number.isFinite(from) || !Number.isFinite(to) || from >= to)
    context.addIssue({ code: 'custom', path: ['to'], message: 'Range end must be after range start.' });
  else if (to - from > 7 * 86_400_000)
    context.addIssue({ code: 'custom', path: ['to'], message: 'Replay range cannot exceed seven days.' });
});
