import { describe, expect, it } from 'vitest';
import { proofDraftSchema, validateEvidenceFile } from './proofOfDeliverySchema';

describe('proofOfDeliverySchema', () => {
  it('accepts absent geolocation and valid paired coordinates', () => {
    expect(proofDraftSchema.safeParse({}).success).toBe(true);
    expect(proofDraftSchema.safeParse({ latitude: 6.9, longitude: 79.8, accuracyMeters: 8 }).success).toBe(true);
  });
  it('rejects partial and out-of-range geolocation', () => {
    expect(proofDraftSchema.safeParse({ latitude: 6.9 }).success).toBe(false);
    expect(proofDraftSchema.safeParse({ latitude: 91, longitude: 10 }).success).toBe(false);
  });
  it('enforces frozen file types and limits', () => {
    expect(validateEvidenceFile('SIGNATURE', new File([new Uint8Array(8)], 'proof.png', { type: 'image/png' }))).toBeNull();
    expect(validateEvidenceFile('PHOTO', new File([new Uint8Array(8)], 'proof.pdf', { type: 'application/pdf' }))).toMatch(/PNG or JPEG/);
    expect(validateEvidenceFile('SIGNATURE', new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'large.jpg', { type: 'image/jpeg' }))).toMatch(/file-size/);
  });
});
