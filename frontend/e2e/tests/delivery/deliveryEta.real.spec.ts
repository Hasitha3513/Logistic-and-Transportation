import { expect, request, test } from '@playwright/test';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const suffix = `e2e-${Date.now()}`;
const customerId = '32000000-0000-0000-0000-000000000001';
const originLocationId = '33000000-0000-0000-0000-000000000001';

type Auth = { accessToken: string; refreshToken: string };
type Rider = { id: string; primaryZoneId: string; transportMode: string; secondaryZoneIds: string[]; maxConcurrentDeliveries: number };
type Batch = { id: string; batchCode: string };
type TenantFixture = { tenantId: string; username: string; password: string };

async function login(username = 'admin', password = 'AdminPass!2026') {
  const context = await request.newContext({ baseURL: backend });
  const response = await context.post('/api/auth/login', { data: { username, password } });
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
  let missingCoordsOrderId: string;
  let tenantBOrderId: string;
  let tenantBBatchId: string;
  let limitedAuth: Auth;
  let motorbikeDuration: number;

  test.beforeAll(async () => {
    auth = await login();
    const context = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } });
    const currentUser = await context.get('/api/auth/me');
    expect(currentUser.ok()).toBeTruthy();
    const currentUserBody = await currentUser.json() as { permissions: string[] };
    expect(currentUserBody.permissions, JSON.stringify(currentUserBody)).toContain('DELIVERY_RIDER_CREATE');

    // Destination 1: with valid coordinates
    const destinationResponse = await context.post('/api/locations', { data: {
      code: `ETA-DEST-${suffix}`.toUpperCase().slice(0, 60), name: `ETA destination ${suffix}`,
      address: 'Real E2E fixture', latitude: 6.90, longitude: 79.85, active: true,
    } });
    expect(destinationResponse.status()).toBe(201);
    const destination = await destinationResponse.json() as { id: string };

    // Destination 2: Missing coordinates fixture
    const missingCoordsDestResponse = await context.post('/api/locations', { data: {
      code: `ETA-NODEST-${suffix}`.toUpperCase().slice(0, 60), name: `ETA no coords ${suffix}`,
      address: 'No coords fixture', active: true,
    } });
    expect(missingCoordsDestResponse.status()).toBe(201);
    const missingCoordsDest = await missingCoordsDestResponse.json() as { id: string };

    // Delivery Zone
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

    // Driver and Rider
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

    // Order 1: Standard valid order
    const orderResponse = await context.post('/api/v1/deliveries', { data: {
      customerId, originLocationId, destinationLocationId: destination.id, priority: 'NORMAL', serviceType: 'STANDARD',
      windowStart: now.toISOString(), windowEnd: new Date(now.getTime() + 2 * 60 * 60 * 1000).toISOString(), instructions: 'Real ETA fixture',
    } });
    expect(orderResponse.status()).toBe(201);
    const order = await orderResponse.json() as { id: string; version: number };
    orderId = order.id;
    const readyResponse = await context.post(`/api/v1/deliveries/${order.id}/validate-readiness`, { data: { version: order.version } });
    expect(readyResponse.ok()).toBeTruthy();

    // Order 2: Missing coordinates order
    const missingCoordsOrderResponse = await context.post('/api/v1/deliveries', { data: {
      customerId, originLocationId, destinationLocationId: missingCoordsDest.id, priority: 'NORMAL', serviceType: 'STANDARD',
      windowStart: now.toISOString(), windowEnd: new Date(now.getTime() + 2 * 60 * 60 * 1000).toISOString(), instructions: 'Missing coordinates fixture',
    } });
    expect(missingCoordsOrderResponse.status()).toBe(201);
    const missingOrder = await missingCoordsOrderResponse.json() as { id: string; version: number };
    missingCoordsOrderId = missingOrder.id;

    // Batch with Order 1
    const batchResponse = await context.post('/api/api/v1/deliveries/batches', { data: {
      deliveryZoneId: zone.id, maxBatchSize: 5, deliveryOrderIds: [order.id], riderId: rider.id,
    } });
    expect(batchResponse.status(), await batchResponse.text()).toBe(201);
    batch = await batchResponse.json() as Batch;

    // Profile-restricted fixture creates a real Tenant-B membership. All operational data below is then created through normal APIs.
    const tenantBResponse = await context.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(tenantBResponse.status()).toBe(201);
    const tenantB = await tenantBResponse.json() as TenantFixture;
    const tenantBAuth = await login(tenantB.username, tenantB.password);
    const tenantBContext = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${tenantBAuth.accessToken}` } });
    const bCustomer = await tenantBContext.post('/api/customers', { data: { code: `ETA-CUST-${suffix}`.slice(0, 40), name: 'E2E tenant B customer', active: true } });
    expect(bCustomer.status()).toBe(201);
    const bCustomerId = (await bCustomer.json() as { id: string }).id;
    const bOrigin = await tenantBContext.post('/api/locations', { data: { code: `ETA-ORIG-${suffix}`.slice(0, 40), name: 'E2E tenant B origin', latitude: 6.86, longitude: 79.81, active: true } });
    const bDestination = await tenantBContext.post('/api/locations', { data: { code: `ETA-DESTB-${suffix}`.slice(0, 40), name: 'E2E tenant B destination', latitude: 6.90, longitude: 79.85, active: true } });
    expect(bOrigin.status()).toBe(201); expect(bDestination.status()).toBe(201);
    const bOriginId = (await bOrigin.json() as { id: string }).id;
    const bDestinationId = (await bDestination.json() as { id: string }).id;
    const bZone = await tenantBContext.post('/api/v1/delivery-zones', { data: { zoneCode: `ETAB-${suffix}`.slice(0, 30), zoneName: 'E2E tenant B zone', zoneType: 'URBAN_DENSE', serviceable: true, dailyCapacity: 5, depotLocationId: bOriginId, coordinates: [{ longitude: 79.80, latitude: 6.85 }, { longitude: 79.90, latitude: 6.85 }, { longitude: 79.90, latitude: 6.98 }, { longitude: 79.80, latitude: 6.85 }], priority: 1 } });
    expect(bZone.status()).toBe(201); const bZoneId = (await bZone.json() as { id: string }).id;
    const bDriver = await tenantBContext.post('/api/drivers', { data: { employeeNumber: `ETAB-${suffix}`.slice(0, 60), firstName: 'E2E', lastName: 'TenantB', status: 'AVAILABLE', active: true } });
    expect(bDriver.status()).toBe(201);
    const bRider = await tenantBContext.post('/api/api/v1/delivery-riders', { data: { riderCode: `ETAB-${suffix}`.slice(0, 40), driverId: (await bDriver.json() as { id: string }).id, riderType: 'FULL_TIME', transportMode: 'MOTORBIKE', primaryZoneId: bZoneId, secondaryZoneIds: [], maxConcurrentDeliveries: 5 } });
    expect(bRider.status()).toBe(201);
    const bOrder = await tenantBContext.post('/api/v1/deliveries', { data: { customerId: bCustomerId, originLocationId: bOriginId, destinationLocationId: bDestinationId, priority: 'NORMAL', serviceType: 'STANDARD', windowStart: now.toISOString(), windowEnd: new Date(now.getTime() + 7_200_000).toISOString() } });
    expect(bOrder.status()).toBe(201); const bOrderBody = await bOrder.json() as { id: string; version: number }; tenantBOrderId = bOrderBody.id;
    expect((await tenantBContext.post(`/api/v1/deliveries/${tenantBOrderId}/validate-readiness`, { data: { version: bOrderBody.version } })).ok()).toBeTruthy();
    const bBatch = await tenantBContext.post('/api/api/v1/deliveries/batches', { data: { deliveryZoneId: bZoneId, maxBatchSize: 5, deliveryOrderIds: [tenantBOrderId], riderId: (await bRider.json() as { id: string }).id } });
    expect(bBatch.status(), await bBatch.text()).toBe(201); tenantBBatchId = (await bBatch.json() as Batch).id;
    await tenantBContext.dispose();

    const limitedRole = await context.post('/api/roles', { data: { name: `E2E ETA VIEWER ${suffix}`, description: 'E2E view-only ETA role', active: true, permissions: ['DELIVERY_VIEW', 'DASHBOARD_VIEW'] } });
    expect(limitedRole.status()).toBe(201);
    const limitedRoleId = (await limitedRole.json() as { id: string }).id;
    const limitedUser = await context.post('/api/users', { data: { username: `e2e-viewer-${suffix}`, email: `e2e-viewer-${suffix}@example.test`, password: `E2e!Viewer-${suffix}`, firstName: 'E2E', lastName: 'Viewer', active: true, roleIds: [limitedRoleId] } });
    expect(limitedUser.status()).toBe(201);
    limitedAuth = await login(`e2e-viewer-${suffix}`, `E2e!Viewer-${suffix}`);
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
    await page.locator('#delivery-eta').getByRole('button', { name: 'Recalculate' }).click();
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

  test('handles missing coordinates gracefully in browser and backend', async ({ page }) => {
    await page.addInitScript(({ accessToken, refreshToken }) => {
      localStorage.setItem('transport.accessToken', accessToken);
      localStorage.setItem('transport.refreshToken', refreshToken);
    }, auth);
    await page.goto(`/deliveries/${missingCoordsOrderId}`);
    await expect(page.getByText('Estimated Arrival')).toBeVisible();

    // The order destination has no coordinates, so ETA query/calculation returns safe unavailable state
    await expect(page.getByText('ETA unavailable')).toBeVisible();

    // Verify direct API returns 400 with DELIVERY_ETA_COORDINATES_MISSING code
    const context = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } });
    const directCalc = await context.post(`/api/api/v1/deliveries/orders/${missingCoordsOrderId}/eta/calculate`);
    expect(directCalc.status()).toBe(400);
    const body = await directCalc.json() as { code: string };
    expect(body.code).toBe('DELIVERY_ETA_COORDINATES_MISSING');
    await context.dispose();
  });

  test('enforces Tenant IDOR protection and rejects cross-tenant ETA access and tenant spoofing', async () => {
    const context = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } });

    const foreignOrderId = tenantBOrderId;
    const foreignBatchId = tenantBBatchId;

    const orderGet = await context.get(`/api/api/v1/deliveries/orders/${foreignOrderId}/eta`);
    expect(orderGet.status()).toBe(404);

    const orderCalc = await context.post(`/api/api/v1/deliveries/orders/${foreignOrderId}/eta/calculate`);
    expect(orderCalc.status()).toBe(404);

    const batchGet = await context.get(`/api/api/v1/deliveries/batches/${foreignBatchId}/eta`);
    expect(batchGet.status()).toBe(404);

    const batchCalc = await context.post(`/api/api/v1/deliveries/batches/${foreignBatchId}/eta/calculate`);
    expect(batchCalc.status()).toBe(404);

    // Tenant spoofing attempt with X-Tenant-Id header on valid resource - authenticated session remains authoritative
    const spoofOrderGet = await context.get(`/api/api/v1/deliveries/orders/${orderId}/eta`, {
      headers: { 'X-Tenant-Id': '00000000-0000-0000-0000-000000000000' },
    });
    expect(spoofOrderGet.ok()).toBeTruthy();

    await context.dispose();
  });

  test('enforces RBAC on ETA recalculation: limited user without DELIVERY_UPDATE receives 403 on direct POST', async ({ page }) => {
    await page.addInitScript(({ accessToken, refreshToken }) => {
      localStorage.setItem('transport.accessToken', accessToken);
      localStorage.setItem('transport.refreshToken', refreshToken);
    }, limitedAuth);

    await page.goto(`/deliveries/${orderId}`);
    await expect(page.getByText('Estimated Arrival')).toBeVisible();

    // Recalculate button is hidden in UI for users lacking DELIVERY_UPDATE
    await expect(page.getByRole('button', { name: 'Recalculate' })).toHaveCount(0);

    const context = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${limitedAuth.accessToken}` } });
    const directPostNoAuth = await context.post(`/api/api/v1/deliveries/orders/${orderId}/eta/calculate`);
    expect(directPostNoAuth.status()).toBe(403);
    await context.dispose();
  });
});
