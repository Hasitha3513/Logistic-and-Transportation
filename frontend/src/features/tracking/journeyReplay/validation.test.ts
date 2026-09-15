import { describe, expect, it } from 'vitest';
import { replayFormSchema } from './validation';
const base={selectorType:'VEHICLE' as const,selectorId:'11111111-1111-4111-8111-111111111111'};
describe('replayFormSchema',()=>{
  it('accepts an ordered seven-day range',()=>expect(replayFormSchema.safeParse({...base,from:'2026-01-01T00:00',to:'2026-01-08T00:00'}).success).toBe(true));
  it('rejects reversed and oversized ranges',()=>{expect(replayFormSchema.safeParse({...base,from:'2026-01-02T00:00',to:'2026-01-01T00:00'}).success).toBe(false);expect(replayFormSchema.safeParse({...base,from:'2026-01-01T00:00',to:'2026-01-09T00:00'}).success).toBe(false)});
});
