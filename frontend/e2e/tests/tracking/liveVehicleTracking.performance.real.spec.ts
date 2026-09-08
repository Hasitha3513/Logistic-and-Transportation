import { createHmac } from 'node:crypto';
import { expect, request, test } from '@playwright/test';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const tenant = '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a';
const provider = 'FIXTURE';
const secret = 'us48-controlled-provider-secret';
const suffix = `us48-load-${Date.now()}`;

test('accepts 200 msg/s sustained and a 1,000 msg/s burst through signed HTTP ingress', async () => {
  const admin = await login();
  const api = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${admin}` } });
  const categories = entities(await ok(api.get('/api/vehicle-categories')));
  const types = entities(await ok(api.get('/api/vehicle-types')));
  const devices: string[] = [];
  for (let index = 0; index < 2; index++) {
    const vehicleResponse = await api.post('/api/vehicles', { data: {
      registrationNumber: `${suffix}-${index}`.toUpperCase(), categoryId: categories[0].id, typeId: types[0].id,
      chassisNumber: `${suffix}-CH-${index}`.toUpperCase(), engineNumber: `${suffix}-EN-${index}`.toUpperCase(),
      manufacturer: 'Acceptance', model: 'US48 Load', manufactureYear: 2026,
      ownershipType: 'COMPANY_OWNED', operationalStatus: 'AVAILABLE', active: true,
    } });
    expect(vehicleResponse.status(), await vehicleResponse.text()).toBe(201);
    const vehicleId = (await vehicleResponse.json() as { id: string }).id;
    const deviceResponse = await api.post('/api/v1/tracking/devices', { data: {
      externalDeviceReference: `${suffix}-${index}`, providerAlias: provider,
      hardwareSerialReference: `${suffix}-serial-${index}`,
    } });
    expect(deviceResponse.status(), await deviceResponse.text()).toBe(201);
    const deviceId = (await deviceResponse.json() as { id: string }).id;
    const association = await api.post(`/api/v1/tracking/devices/${deviceId}/associations`, { data: {
      vehicleId, effectiveFrom: new Date(Date.now() - 60_000).toISOString(),
    } });
    expect(association.status(), await association.text()).toBe(201);
    devices.push(deviceId);
  }

  const sustainedStart = performance.now();
  const sustained = await signed(batch(devices[0], 0, 200));
  const sustainedMillis = performance.now() - sustainedStart;
  console.log(`US48_SUSTAINED messages=200 elapsedMs=${sustainedMillis.toFixed(1)} rate=${(200 / (sustainedMillis / 1000)).toFixed(1)}msg/s`);
  expect(sustained.status(), await sustained.text()).toBe(200);
  expect(200 / (sustainedMillis / 1000)).toBeGreaterThanOrEqual(200);

  const burstStart = performance.now();
  const burst = await Promise.all([signed(batch(devices[0], 200, 500)), signed(batch(devices[1], 700, 500))]);
  const burstMillis = performance.now() - burstStart;
  console.log(`US48_BURST messages=1000 elapsedMs=${burstMillis.toFixed(1)} rate=${(1000 / (burstMillis / 1000)).toFixed(1)}msg/s`);
  for (const response of burst) expect(response.status(), await response.text()).toBe(200);
  expect(1000 / (burstMillis / 1000)).toBeGreaterThanOrEqual(1000);
  await api.dispose();
});

function batch(deviceId: string, offset: number, size: number) {
  const source = Date.now();
  return Array.from({ length: size }, (_, index) => ({
    deviceId, providerMessageId: `${suffix}-${offset + index}`, providerSequence: offset + index,
    sourceTimestamp: new Date(source + index).toISOString(), latitude: 6.9271, longitude: 79.8612,
    horizontalAccuracyMeters: 5,
  }));
}

async function signed(payload: unknown) {
  const body = JSON.stringify(payload); const epoch = Math.floor(Date.now() / 1000); const nonce = crypto.randomUUID();
  const signature = createHmac('sha256', secret).update(`${epoch}\n${nonce}\n${tenant}\n${body}`).digest('hex');
  const api = await request.newContext({ baseURL: backend });
  return api.post('/api/integration/v1/tracking/positions', { headers: {
    'Content-Type': 'application/json', 'X-Tracking-Tenant': tenant, 'X-Tracking-Provider': provider,
    'X-Tracking-Timestamp': String(epoch), 'X-Tracking-Nonce': nonce, 'X-Tracking-Signature': signature,
  }, data: body });
}

async function login() {
  const api = await request.newContext({ baseURL: backend });
  const response = await api.post('/api/auth/login', { data: { username: 'admin', password: 'AdminPass!2026' } });
  expect(response.status(), await response.text()).toBe(200);
  return (await response.json() as { accessToken: string }).accessToken;
}
async function ok(promise: Promise<import('@playwright/test').APIResponse>) { const response = await promise; expect(response.status(), await response.text()).toBe(200); return response.json(); }
function entities(value: unknown): Array<{ id: string }> { return Array.isArray(value) ? value as Array<{ id: string }> : ((value as { content?: Array<{ id: string }> }).content ?? []); }
