import { expect, request, test } from '@playwright/test';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const suffix = `e2e-${Date.now()}`;
const customerId = '10000000-0000-0000-0000-000000000001';
const originLocationId = '20000000-0000-0000-0000-000000000001';

type Auth = { accessToken: string; refreshToken: string };
type Rider = { id: string; primaryZoneId: string; transportMode: string; secondaryZoneIds: string[]; maxConcurrentDeliveries: number };
type Batch = { id: string; batchCode: string };

async function login() {
  const context = await request.newContext({ baseURL: backend });
  const response = await context.post('/api/auth/login', { data: { username: 'admin', password: 'AdminPass!2026' } });
  expect(response.ok()).toBeTruthy();
  const auth = await response.json() as Auth;
  await context.dispose();
  return auth;
}

test.describe.serial('US-67 real PostgreSQL Rider ETA acceptance', () => {
  let auth: Auth;
  let rider: Rider;
  let batch: Batch;
  let orderId: string;
  let motorbikeDuration: number;

  test.beforeAll(async () => {
    auth = await login();
    const context = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } });
    const destinationResponse = await context.post('/api/locations', { data: {
      code: `ETA-DEST-${suffix}`.toUpperCase().slice(0, 60), name: `ETA destination ${suffix}`,
      address: 'Real E2E fixture', latitude: 6.90, longitude: 79.85, active: true,
    } });
    expect(destinationResponse.status()).toBe(201);
    const destination = await destinationResponse.json() as { id: string };
    const zoneResponse = await context.post('/api/v1/delivery-zones', { data: {
      zoneCode: `ETA-${suffix}`.toUpperCase().slice(0, 30), zoneName: `ETA ${suffix}`, description: 'Real US-67 browser fixture',
      zoneType: 'URBAN_DENSE', serviceable: true, dailyCapacity: 10, depotLocationId: originLocationId,
      coordinates: [
        { longitude: 79.80, latitude: 6.85 }, { longitude: 79.90, latitude: 6.85 },
        { longitude: 79.90, latitude: 6.98 }, { longitude: 79.80, latitude: 6.85 },
      ], priority: 1,
    } });
    expect(zoneResponse.status()).toBe(201);
    const zone = await zoneResponse.json() as { id: string };

    const driverResponse = await context.post('/api/drivers', { data: {
      employeeNumber: `ETA-${suffix}`.toUpperCase().slice(0, 60), firstName: 'ETA', lastName: 'Rider',
      email: `${suffix}@example.test`, status: 'AVAILABLE', active: true,
    } });
    expect(driverResponse.status()).toBe(201);
    const driver = await driverResponse.json() as { id: string };
    const riderResponse = await context.post('/api/api/v1/delivery-riders', { data: {
      riderCode: `ETA-${suffix}`.toUpperCase().slice(0, 40), driverId: driver.id, riderType: 'FULL_TIME', transportMode: 'MOTORBIKE',
      primaryZoneId: zone.id, secondaryZoneIds: [], maxConcurrentDeliveries: 5,
    } });
    expect(riderResponse.status(), await riderResponse.text()).toBe(201);
    rider = await riderResponse.json() as Rider;

    const now = new Date();
    const orderResponse = await context.post('/api/v1/deliveries', { data: {
      customerId, originLocationId, destinationLocationId: destination.id, priority: 'NORMAL', serviceType: 'STANDARD',
      windowStart: now.toISOString(), windowEnd: new Date(now.getTime() + 2 * 60 * 60 * 1000).toISOString(), instructions: 'Real ETA fixture',
    } });
    expect(orderResponse.status()).toBe(201);
    const order = await orderResponse.json() as { id: string; version: number };
    orderId = order.id;
    const readyResponse = await context.post(`/api/v1/deliveries/${order.id}/validate-readiness`, { data: { version: order.version } });
    expect(readyResponse.ok()).toBeTruthy();

    const batchResponse = await context.post('/api/api/v1/deliveries/batches', { data: {
      deliveryZoneId: zone.id, maxBatchSize: 5, deliveryOrderIds: [order.id], riderId: rider.id,
    } });
    expect(batchResponse.status(), await batchResponse.text()).toBe(201);
    batch = await batchResponse.json() as Batch;
    await context.dispose();
  });

  test('calculates MOTORBIKE ETA through the real UI and backend', async ({ page }) => {
    await page.addInitScript(({ accessToken, refreshToken }) => {
      localStorage.setItem('transport.accessToken', accessToken);
      localStorage.setItem('transport.refreshToken', refreshToken);
    }, auth);
    await page.goto('/deliveries/batches');
    await expect(page.getByText(batch.batchCode)).toBeVisible();
    await page.getByRole('button', { name: /Details/ }).filter({ has: page.locator('..') }).first().click();
    await page.getByRole('button', { name: 'Recalculate ETA' }).click();
    await expect(page.getByText('HEURISTIC', { exact: true })).toBeVisible();
    const duration = await page.getByText(/mins/).first().textContent();
    motorbikeDuration = Number.parseInt(duration ?? '', 10);
    expect(motorbikeDuration).toBeGreaterThan(0);
    await page.reload();
    await expect(page.getByText(batch.batchCode)).toBeVisible();
  });

  test('shows and recalculates the single-order ETA through the real UI', async ({ page }) => {
    await page.addInitScript(({ accessToken, refreshToken }) => {
      localStorage.setItem('transport.accessToken', accessToken);
      localStorage.setItem('transport.refreshToken', refreshToken);
    }, auth);
    await page.goto(`/deliveries/${orderId}`);
    await expect(page.getByText('Estimated Arrival')).toBeVisible();
    await expect(page.getByText('Travel Duration')).toBeVisible();
    await expect(page.getByText('Distance')).toBeVisible();
    await expect(page.getByText('SLA Status')).toBeVisible();
    await expect(page.getByText('Calculated At')).toBeVisible();
    await expect(page.getByText('Fresh/Stale')).toBeVisible();
    await page.getByRole('button', { name: 'Recalculate' }).click();
    await expect(page.getByText('Source: HEURISTIC', { exact: true })).toBeVisible();
  });

  test('changes Rider mode through the real API and receives a different BICYCLE ETA', async () => {
    const context = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } });
    const update = await context.put(`/api/api/v1/delivery-riders/${rider.id}`, { data: {
      primaryZoneId: rider.primaryZoneId, transportMode: 'BICYCLE', secondaryZoneIds: rider.secondaryZoneIds,
      maxConcurrentDeliveries: rider.maxConcurrentDeliveries,
    } });
    expect(update.ok()).toBeTruthy();
    expect((await update.json() as Rider).transportMode).toBe('BICYCLE');
    const eta = await context.post(`/api/api/v1/deliveries/batches/${batch.id}/eta/calculate`);
    expect(eta.ok()).toBeTruthy();
    const recalculated = await eta.json() as { totalDurationSeconds: number; source: string };
    expect(recalculated.source).toBe('HEURISTIC');
    expect(recalculated.totalDurationSeconds).toBeGreaterThan(motorbikeDuration * 60);
    await context.dispose();
  });
});
