import { describe, expect, it } from 'vitest';
import { deliveryOrderSchema } from './deliveryOrderSchema';

const valid = { customerId: '11111111-1111-4111-8111-111111111111', originLocationId: '22222222-2222-4222-8222-222222222222', destinationLocationId: '33333333-3333-4333-8333-333333333333', priority: 'NORMAL' as const, serviceType: 'STANDARD' as const, windowStart: '2026-08-29T10:00:00Z', windowEnd: '2026-08-29T12:00:00Z', instructions: '' };
describe('deliveryOrderSchema', () => {
  it('accepts the frozen defaults and complete requirements', () => expect(deliveryOrderSchema.safeParse(valid).success).toBe(true));
  it('rejects a delivery window ending before it starts', () => expect(deliveryOrderSchema.safeParse({ ...valid, windowEnd: '2026-08-29T09:59:59Z' }).success).toBe(false));
  it('rejects identifiers that cannot represent governed references', () => expect(deliveryOrderSchema.safeParse({ ...valid, customerId: '' }).success).toBe(false));
});
