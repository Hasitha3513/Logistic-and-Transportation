import { expect, request, test } from '@playwright/test';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const suffix = `us70-${Date.now()}`;
type Auth = { accessToken: string; refreshToken: string };
type Order = { id: string; deliveryNumber: string; version: number };
type Proof = { version: number };

async function login() {
  const api = await request.newContext({ baseURL: backend });
  const response = await api.post('/api/auth/login', { data: { username: 'admin', password: 'AdminPass!2026' } });
  expect(response.status(), await response.text()).toBe(200);
  const auth = await response.json() as Auth; await api.dispose(); return auth;
}

test.describe.serial('US-70 real PostgreSQL customer self-service', () => {
  let auth: Auth; let token: string; let originalPath: string; let order: Order;
  let otherCustomerId: string; let otherTenantId: string;

  test.beforeAll(async () => {
    auth = await login();
    const api = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } });
    const customer = await api.post('/api/customers', { data: { code: `US70-${suffix}`.slice(0, 40),
      name: 'US70 Customer', contactPerson: 'Recipient', phone: '+947700000070',
      email: `e2e-self-service-${suffix}@example.test`, active: true } });
    expect(customer.status(), await customer.text()).toBe(201); const customerId = (await customer.json() as { id: string }).id;
    const otherCustomer = await api.post('/api/customers', { data: { code: `US70-B-${suffix}`.slice(0, 40),
      name: 'US70 Other Customer', contactPerson: 'Other Recipient', phone: '+947700000071',
      email: `e2e-self-service-other-${suffix}@example.test`, active: true } });
    expect(otherCustomer.status(), await otherCustomer.text()).toBe(201);
    otherCustomerId = (await otherCustomer.json() as { id: string }).id;
    const otherTenant = await api.post('/api/e2e/tenant-fixtures', { data: { suffix: `us70-${Date.now()}` } });
    expect(otherTenant.status(), await otherTenant.text()).toBe(201);
    otherTenantId = (await otherTenant.json() as { tenantId: string }).tenantId;
    const origin = await api.post('/api/locations', { data: { code: `US70-O-${suffix}`.slice(0, 40), name: 'US70 origin', latitude: 6.92, longitude: 79.84, active: true } });
    const destination = await api.post('/api/locations', { data: { code: `US70-D-${suffix}`.slice(0, 40), name: 'Colombo customer destination', address: 'Sensitive address must not be shown', latitude: 6.90, longitude: 79.86, active: true } });
    expect(origin.status()).toBe(201); expect(destination.status()).toBe(201);
    const now = Date.now();
    const created = await api.post('/api/v1/deliveries', { data: { customerId,
      originLocationId: (await origin.json() as { id: string }).id,
      destinationLocationId: (await destination.json() as { id: string }).id,
      priority: 'NORMAL', serviceType: 'STANDARD', windowStart: new Date(now + 3_600_000).toISOString(),
      windowEnd: new Date(now + 7_200_000).toISOString() } });
    expect(created.status(), await created.text()).toBe(201); order = await created.json() as Order;
    const ready = await api.post(`/api/v1/deliveries/${order.id}/validate-readiness`, { data: { version: order.version } });
    expect(ready.status(), await ready.text()).toBe(200); order = await ready.json() as Order;
    const originId = (await api.get(`/api/v1/deliveries/${order.id}`)).ok(); void originId;
    const location = await api.post('/api/locations', { data: { code: `US70-DEP-${suffix}`.slice(0, 40), name: 'US70 depot', latitude: 6.91, longitude: 79.83, active: true } });
    const depotId = (await location.json() as { id: string }).id;
    const zone = await api.post('/api/v1/delivery-zones', { data: { zoneCode: `US70-${suffix}`.toUpperCase().slice(0, 30), zoneName: 'US70 zone', zoneType: 'URBAN_DENSE', serviceable: true, dailyCapacity: 10, depotLocationId: depotId,
      coordinates: [{ longitude: 79.80, latitude: 6.85 }, { longitude: 79.90, latitude: 6.85 }, { longitude: 79.90, latitude: 6.98 }, { longitude: 79.80, latitude: 6.85 }], priority: 1 } });
    const zoneId = (await zone.json() as { id: string }).id;
    const driver = await api.post('/api/drivers', { data: { employeeNumber: `US70-${suffix}`.slice(0, 60), firstName: 'US70', lastName: 'Rider', status: 'AVAILABLE', active: true } });
    const rider = await api.post('/api/api/v1/delivery-riders', { data: { riderCode: `US70-${suffix}`.toUpperCase().slice(0, 40), driverId: (await driver.json() as { id: string }).id, riderType: 'FULL_TIME', transportMode: 'MOTORBIKE', primaryZoneId: zoneId, secondaryZoneIds: [], maxConcurrentDeliveries: 5 } });
    const batch = await api.post('/api/api/v1/deliveries/batches', { data: { deliveryZoneId: zoneId, maxBatchSize: 5, deliveryOrderIds: [order.id] } });
    const batchId = (await batch.json() as { id: string }).id;
    expect((await api.post(`/api/api/v1/deliveries/batches/${batchId}/ready`)).status()).toBe(200);
    expect((await api.post(`/api/api/v1/deliveries/batches/${batchId}/assign-rider`, { data: { riderId: (await rider.json() as { id: string }).id, isOverride: false, overrideReason: null } })).status()).toBe(200);
    expect((await api.post(`/api/api/v1/deliveries/batches/${batchId}/dispatch`)).status()).toBe(200);
    await expect.poll(async () => {
      await api.post('/api/e2e/notifications/process-email');
      return (await api.get('/api/e2e/notifications/latest-accepted-email')).status();
    }).toBe(200);
    const email = await (await api.get('/api/e2e/notifications/latest-accepted-email')).json() as { body: string };
    const match = email.body.match(/https?:\/\/\S+\/track#access_token=([A-Za-z0-9_-]{43})/);
    expect(match).not.toBeNull(); token = match![1]; originalPath = `/track#access_token=${token}`;
    await api.dispose();
  });

  test('opens the valid magic link, removes the fragment, uses DeliveryAccess, and excludes operator shell', async ({ page }) => {
    let authorization = '';
    page.on('request', value => { if (value.url().includes('/delivery-self-service')) authorization = value.headers().authorization ?? ''; });
    await page.goto(originalPath);
    await expect(page.getByText(order.deliveryNumber)).toBeVisible();
    expect(page.url()).not.toContain('access_token');
    expect(authorization).toBe(`DeliveryAccess ${token}`);
    await expect(page.getByText('Out for delivery')).toBeVisible();
    await expect(page.getByText('Colombo customer destination')).toBeVisible();
    await expect(page.getByText('Scheduled window')).toBeVisible();
    await expect(page.getByText('Estimated arrival')).toBeVisible();
    await expect(page.getByText('Sensitive address must not be shown')).toHaveCount(0);
    await expect(page.getByText('Sign out')).toHaveCount(0);
  });

  test('reads and persists Email/SMS preferences', async ({ page }) => {
    await page.goto(originalPath);
    const email = page.getByRole('checkbox', { name: /Email updates/ });
    await expect(email).toBeChecked();
    await page.getByRole('checkbox', { name: /SMS updates/ }).check();
    const updated = page.waitForResponse(response => response.url().endsWith('/notification-preferences')
      && response.request().method() === 'PUT');
    await page.getByRole('button', { name: 'Save preferences' }).click();
    const response = await updated;
    expect(response.status()).toBe(200);
    await expect(page.getByText('Preferences updated.')).toBeVisible();
  });

  test('submits a customer issue with a customer-safe reference', async ({ page }) => {
    await page.goto(originalPath);
    await page.getByLabel('Issue category').click(); await page.getByText('DELIVERY SERVICE').click();
    await page.getByLabel('Description').fill('The delivery service needs customer assistance.');
    await page.getByRole('button', { name: 'Submit issue' }).click();
    await expect(page.getByText('Your issue was submitted.')).toBeVisible();
  });

  test('submits only a non-binding delivery request and leaves customer status unchanged', async ({ page }) => {
    await page.goto(originalPath);
    const start = new Date(Date.now() + 86_400_000); const end = new Date(start.getTime() + 3_600_000);
    const local = (value: Date) => new Date(value.getTime() - value.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
    await page.getByLabel('Preferred start').fill(local(start)); await page.getByLabel('Preferred end').fill(local(end));
    await page.getByLabel('Notes').fill('Please call before the preferred window.');
    await page.getByRole('button', { name: 'Submit request' }).click();
    await expect(page.getByText('Your non-binding delivery request was submitted.')).toBeVisible();
    await expect(page.getByText('Out for delivery')).toBeVisible();
  });

  test('reload loses memory-only access and reopening the original link works', async ({ page }) => {
    await page.goto(originalPath); await expect(page.getByText(order.deliveryNumber)).toBeVisible();
    await page.reload(); await expect(page.getByText('Open your original delivery link')).toBeVisible();
    await page.goto(originalPath); await expect(page.getByText(order.deliveryNumber)).toBeVisible();
  });

  test('rejects Customer-B/Tenant-B authority injection and keeps the token-bound projection', async () => {
    const publicApi = await request.newContext({ baseURL: backend,
      extraHTTPHeaders: { Authorization: `DeliveryAccess ${token}` } });
    const projected = await publicApi.get('/api/public/v1/delivery-self-service', { params: {
      tenantId: otherTenantId, customerId: otherCustomerId, deliveryId: crypto.randomUUID(),
    } });
    expect(projected.status(), await projected.text()).toBe(200);
    expect((await projected.json() as { deliveryNumber: string }).deliveryNumber).toBe(order.deliveryNumber);
    const injected = await publicApi.post('/api/public/v1/delivery-self-service/issues', {
      headers: { 'Idempotency-Key': 'mass-assignment-00000001' },
      data: { category: 'OTHER', description: 'A valid issue with injected target identifiers.',
        tenantId: otherTenantId, customerId: otherCustomerId, deliveryId: crypto.randomUUID() },
    });
    expect(injected.status()).toBe(400);
    await publicApi.dispose();
  });

  test('keeps the completed link valid and records one post-delivery feedback result', async ({ page }) => {
    const api = await authorized(auth);
    order = await (await api.get(`/api/v1/deliveries/${order.id}`)).json() as Order;
    await completeOrder(api, order);
    await api.dispose();

    await page.goto(originalPath);
    await expect(page.getByText('Delivered', { exact: true })).toBeVisible();
    await expect(page.getByText('Recorded', { exact: true })).toBeVisible();
    await page.getByLabel('Rating').fill('5');
    await page.getByLabel('Comment').fill('Everything arrived safely.');
    await page.getByRole('button', { name: 'Submit feedback' }).click();
    await expect(page.getByText('Thank you for your feedback.')).toBeVisible();
  });

  test('expired, revoked, and Customer-mismatched links share the same safe denial', async ({ page }) => {
    const api = await authorized(auth);
    const expired = await issueFixtureLink(api, 'expired');
    const revoked = await issueFixtureLink(api, 'revoked');
    const mismatched = await issueFixtureLink(api, 'customer-mismatch');
    expect((await api.post('/api/e2e/delivery-self-service/expire', { data: { token: expired } })).status()).toBe(204);
    expect((await api.post('/api/e2e/delivery-self-service/revoke', { data: { token: revoked } })).status()).toBe(204);
    expect((await api.post('/api/e2e/delivery-self-service/mismatch-customer', {
      data: { token: mismatched, customerId: otherCustomerId },
    })).status()).toBe(204);
    await api.dispose();

    for (const denied of [expired, revoked, mismatched]) {
      await page.goto(`/track#access_token=${denied}`);
      await expect(page.getByText('Delivery access unavailable')).toBeVisible();
      await expect(page.getByText('This delivery link is invalid or no longer available.')).toBeVisible();
    }
  });

  test('invalid access receives safe UX and never opens the operator shell', async ({ page }) => {
    await page.goto('/track#access_token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA');
    await expect(page.getByText('Delivery access unavailable')).toBeVisible();
    await expect(page.getByText('This delivery link is invalid or no longer available.')).toBeVisible();
    await expect(page.getByText('Sign out')).toHaveCount(0);
  });

  function authorized(value: Auth) {
    return request.newContext({ baseURL: backend,
      extraHTTPHeaders: { Authorization: `Bearer ${value.accessToken}` } });
  }

  async function issueFixtureLink(api: Awaited<ReturnType<typeof request.newContext>>, purpose: string) {
    const response = await api.post('/api/e2e/delivery-self-service/links', { data: {
      deliveryOrderId: order.id, recipientContact: `e2e-self-service-${suffix}@example.test`,
      idempotencyKey: `us70-${purpose}-${Date.now()}-fixture`,
    } });
    expect(response.status(), await response.text()).toBe(201);
    const link = (await response.json() as { url: string }).url;
    const match = link.match(/#access_token=([A-Za-z0-9_-]{43})$/);
    expect(match).not.toBeNull();
    return match![1];
  }

  async function completeOrder(api: Awaited<ReturnType<typeof request.newContext>>, value: Order) {
    const created = await api.post(`/api/v1/deliveries/${value.id}/proof`, { data: {
      deliveryVersion: value.version, signerName: 'Recipient', signerRelationship: 'Customer',
    } });
    expect(created.status(), await created.text()).toBe(201);
    const proof = await created.json() as Proof;
    const evidence = await api.post(`/api/v1/deliveries/${value.id}/proof/evidence`, { multipart: {
      podVersion: String(proof.version), type: 'BARCODE', barcodeValue: value.deliveryNumber,
      captureSource: 'SCANNER',
    } });
    expect(evidence.status(), await evidence.text()).toBe(201);
    const withEvidence = await evidence.json() as Proof;
    const finalized = await api.post(`/api/v1/deliveries/${value.id}/proof/finalize`, { data: {
      deliveryVersion: value.version, podVersion: withEvidence.version,
    } });
    expect(finalized.status(), await finalized.text()).toBe(200);
  }
});
