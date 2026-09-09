import { describe, expect, it } from 'vitest';
import { providerConnectionSchema } from './providerConnectionSchema';

const valid={providerType:'FLESPI',displayName:'Colombo fleet',providerAlias:'FLESPI_LK',providerKeyId:'provider-key',endpointUri:'https://flespi.io',credentialReference:'env:FLESPI_TOKEN',pollIntervalSeconds:5,pageSize:500,safeConfigurationText:'{"region":"lk"}'};
describe('providerConnectionSchema',()=>{
 it('accepts the bounded safe CS06 contract',()=>expect(providerConnectionSchema.safeParse(valid).success).toBe(true));
 it.each(['http://flespi.io','https://localhost','https://127.0.0.1'])('rejects an obvious unsafe endpoint %s',endpointUri=>expect(providerConnectionSchema.safeParse({...valid,endpointUri}).success).toBe(false));
 it('rejects secret-like safe configuration keys',()=>expect(providerConnectionSchema.safeParse({...valid,safeConfigurationText:'{"apiToken":"no"}'}).success).toBe(false));
 it('rejects non-object safe configuration',()=>expect(providerConnectionSchema.safeParse({...valid,safeConfigurationText:'[]'}).success).toBe(false));
 it('enforces polling and page bounds',()=>expect(providerConnectionSchema.safeParse({...valid,pollIntervalSeconds:4,pageSize:501}).success).toBe(false));
});
