import { describe, expect, it } from 'vitest';
import { integrationSchema } from './integrationSchema';

describe('integrationSchema', () => {
  it('accepts only the server allow-listed endpoint alias', () => {
    expect(integrationSchema.safeParse({ name: 'Sandbox', endpointAlias: 'CONTROLLED_SANDBOX' }).success).toBe(true);
    expect(integrationSchema.safeParse({ name: 'Sandbox', endpointAlias: '../operator/path' }).success).toBe(false);
    expect(integrationSchema.safeParse({ name: 'Sandbox', endpointAlias: 'https://example.com' }).success).toBe(false);
  });
});
