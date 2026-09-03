import { expect, request, test } from '@playwright/test';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const suffix = `us69-${Date.now()}`;

type Auth = { accessToken: string; refreshToken: string };
type TenantFixture = { username: string; password: string };
type Order = { id: string; deliveryNumber: string; version: number };
type Proof = { version: number };
type Batch = { id: string; status: string };
type Notification = { notificationId: string; eventType: string; channel: string; status: string; recipient?: string };

async function login(username = 'admin', password = 'AdminPass!2026') {
  const context = await request.newContext({ baseURL: backend });
  const response = await context.post('/api/auth/login', { data: { username, password } });
  expect(response.status(), await response.text()).toBe(200);
  const auth = await response.json() as Auth;
  await context.dispose();
  return auth;
}

test.describe.serial('US-69 real PostgreSQL Delivery notifications acceptance', () => {
  let admin: Auth;
  let limited: Auth;
  let tenantB: Auth;
  let customerId: string;
  let defaultOrder: Order;
  let smsOrder: Order;
  let dispatchOrder: Order;
  let dispatchBatch: Batch;
  let dispatchRiderId: string;

  test.beforeAll(async () => {
    admin = await login();
    const adminApi = await request.newContext({ baseURL: backend,
      extraHTTPHeaders: { Authorization: `Bearer ${admin.accessToken}` } });
    const customerResponse = await adminApi.post('/api/customers', { data: {
      code: `US69-C-${suffix}`.slice(0, 40), name: 'US69 Acceptance Customer', contactPerson: 'Recipient',
      phone: '+947700000001', email: `customer-${suffix}@example.test`, active: true,
    } });
    expect(customerResponse.status(), await customerResponse.text()).toBe(201);
    customerId = (await customerResponse.json() as { id: string }).id;
    const origin = await adminApi.post('/api/locations', { data: {
      code: `US69-O-${suffix}`.slice(0, 40), name: 'US69 origin', latitude: 6.92, longitude: 79.84, active: true,
    } });
    const destination = await adminApi.post('/api/locations', { data: {
      code: `US69-D-${suffix}`.slice(0, 40), name: 'US69 destination', latitude: 6.90, longitude: 79.86, active: true,
    } });
    expect(origin.status()).toBe(201); expect(destination.status()).toBe(201);
    const originId = (await origin.json() as { id: string }).id;
    const destinationId = (await destination.json() as { id: string }).id;
    defaultOrder = await createReadyOrder(adminApi, originId, destinationId);
    smsOrder = await createReadyOrder(adminApi, originId, destinationId);
    dispatchOrder = await createReadyOrder(adminApi, originId, destinationId);

    const zone = await adminApi.post('/api/v1/delivery-zones', { data: {
      zoneCode: `US69-${suffix}`.toUpperCase().slice(0, 30), zoneName: 'US69 dispatch zone',
      description: 'US69 readiness versus dispatch acceptance fixture', zoneType: 'URBAN_DENSE',
      serviceable: true, dailyCapacity: 10, depotLocationId: originId,
      coordinates: [
        { longitude: 79.80, latitude: 6.85 }, { longitude: 79.90, latitude: 6.85 },
        { longitude: 79.90, latitude: 6.98 }, { longitude: 79.80, latitude: 6.85 },
      ], priority: 1,
    } });
    expect(zone.status(), await zone.text()).toBe(201);
    const zoneId = (await zone.json() as { id: string }).id;
    const driver = await adminApi.post('/api/drivers', { data: {
      employeeNumber: `US69-${suffix}`.toUpperCase().slice(0, 60), firstName: 'US69', lastName: 'Dispatch',
      email: `dispatch-${suffix}@example.test`, status: 'AVAILABLE', active: true,
    } });
    expect(driver.status(), await driver.text()).toBe(201);
    const rider = await adminApi.post('/api/api/v1/delivery-riders', { data: {
      riderCode: `US69-${suffix}`.toUpperCase().slice(0, 40),
      driverId: (await driver.json() as { id: string }).id, riderType: 'FULL_TIME', transportMode: 'MOTORBIKE',
      primaryZoneId: zoneId, secondaryZoneIds: [], maxConcurrentDeliveries: 5,
    } });
    expect(rider.status(), await rider.text()).toBe(201);
    dispatchRiderId = (await rider.json() as { id: string }).id;
    const batch = await adminApi.post('/api/api/v1/deliveries/batches', { data: {
      deliveryZoneId: zoneId, maxBatchSize: 5, deliveryOrderIds: [dispatchOrder.id],
    } });
    expect(batch.status(), await batch.text()).toBe(201);
    dispatchBatch = await batch.json() as Batch;
    expect(dispatchBatch.status).toBe('DRAFT');

    const role = await adminApi.post('/api/roles', { data: {
      name: `US69 limited ${suffix}`, active: true, permissions: ['DELIVERY_VIEW'],
    } });
    const roleId = (await role.json() as { id: string }).id;
    const username = `us69-limited-${suffix}`;
    const password = `Us69Limited!${suffix}`;
    expect((await adminApi.post('/api/users', { data: { username, email: `${username}@example.test`, password,
      firstName: 'US69', lastName: 'Limited', active: true, roleIds: [roleId] } })).status()).toBe(201);
    limited = await login(username, password);

    const tenantResponse = await adminApi.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(tenantResponse.status()).toBe(201);
    const fixture = await tenantResponse.json() as TenantFixture;
    tenantB = await login(fixture.username, fixture.password);
    await adminApi.dispose();
  });

  test('READY emits no customer notice and committed DISPATCHED emits exactly one masked notice', async ({ page }) => {
    const api = await authorized(admin);
    const ready = await api.post(`/api/api/v1/deliveries/batches/${dispatchBatch.id}/ready`);
    expect(ready.status(), await ready.text()).toBe(200);
    expect((await ready.json() as Batch).status).toBe('READY');
    expect((await deliveryHistory(api, dispatchOrder.id))
      .filter(item => item.eventType === 'DELIVERY_OUT_FOR_DELIVERY')).toHaveLength(0);

    const assigned = await api.post(`/api/api/v1/deliveries/batches/${dispatchBatch.id}/assign-rider`, { data: {
      riderId: dispatchRiderId, isOverride: false, overrideReason: null,
    } });
    expect(assigned.status(), await assigned.text()).toBe(200);
    expect((await assigned.json() as Batch).status).toBe('ASSIGNED');
    const dispatched = await api.post(`/api/api/v1/deliveries/batches/${dispatchBatch.id}/dispatch`);
    expect(dispatched.status(), await dispatched.text()).toBe(200);
    expect((await dispatched.json() as Batch).status).toBe('DISPATCHED');

    await expect.poll(async () => (await deliveryHistory(api, dispatchOrder.id))
      .filter(item => item.eventType === 'DELIVERY_OUT_FOR_DELIVERY').length).toBe(1);
    const [notice] = (await deliveryHistory(api, dispatchOrder.id))
      .filter(item => item.eventType === 'DELIVERY_OUT_FOR_DELIVERY');
    expect(notice.channel).toBe('EMAIL');
    expect(notice.recipient).toContain('***');

    await page.addInitScript((auth) => {
      localStorage.setItem('transport.accessToken', auth.accessToken);
      localStorage.setItem('transport.refreshToken', auth.refreshToken);
    }, admin);
    await page.goto(`/deliveries/${dispatchOrder.id}`);
    await expect(page.getByText('Customer notification timeline')).toBeVisible();
    await expect(page.getByText('OUT FOR DELIVERY', { exact: true })).toBeVisible();
    await expect(page.getByText('Destination:', { exact: false })).toContainText('***');
    await api.dispose();
  });

  test('completion emits and consumes the frozen event while SMS defaults off', async () => {
    const api = await authorized(admin);
    await completeOrder(api, defaultOrder);
    const history = await deliveryHistory(api, defaultOrder.id);
    expect(history.some(item => item.eventType === 'DELIVERY_COMPLETED' && item.channel === 'EMAIL')).toBeTruthy();
    expect(history.some(item => item.channel === 'SMS')).toBeFalsy();
    await api.dispose();
  });

  test('default preference is Email on, SMS off, and destinations are masked', async () => {
    const api = await authorized(admin);
    const response = await api.get(`/api/v1/notification-customer-preferences/${customerId}`);
    expect(response.status()).toBe(200);
    const preference = await response.json() as Record<string, unknown>;
    expect(preference).toMatchObject({ explicitProfile: false, emailEnabled: true, smsEnabled: false });
    expect(String(preference.maskedEmail)).toContain('***@');
    expect(String(preference.maskedPhone)).toContain('***');
    expect(JSON.stringify(preference)).not.toContain('+947700000001');
    await api.dispose();
  });

  test('explicit SMS preference produces a deterministic accepted SMS path', async () => {
    const api = await authorized(admin);
    const preference = await api.put(`/api/v1/notification-customer-preferences/${customerId}`, { data: {
      emailEnabled: false, smsEnabled: true, version: null,
    } });
    expect(preference.status(), await preference.text()).toBe(200);
    await completeOrder(api, smsOrder);
    await expect.poll(async () => {
      const history = await deliveryHistory(api, smsOrder.id);
      return history.find(item => item.channel === 'SMS')?.status;
    }).toBe('SENT');
    const history = await deliveryHistory(api, smsOrder.id);
    const sms = history.find(item => item.channel === 'SMS');
    expect(sms?.recipient).toContain('***');
    expect(sms?.recipient).not.toBe('+947700000001');
    await api.dispose();
  });

  test('delivery detail renders the permission-gated masked timeline', async ({ page }) => {
    await page.addInitScript((auth) => {
      localStorage.setItem('transport.accessToken', auth.accessToken);
      localStorage.setItem('transport.refreshToken', auth.refreshToken);
    }, admin);
    await page.goto(`/deliveries/${smsOrder.id}`);
    await expect(page.getByText('Customer notification timeline')).toBeVisible();
    await expect(page.getByText('Delivery Completed', { exact: true })).toBeVisible();
    await expect(page.getByText('Destination:', { exact: false })).toContainText('***');
  });

  test('tenant B cannot read another tenant preference or history', async () => {
    const api = await authorized(tenantB);
    expect((await api.get(`/api/v1/notification-customer-preferences/${customerId}`)).status()).toBe(404);
    expect(await deliveryHistory(api, smsOrder.id)).toEqual([]);
    await api.dispose();
  });

  test('insufficient permission is denied on literal history and preference URLs', async () => {
    const api = await authorized(limited);
    expect((await api.get(`/api/v1/notification-deliveries?aggregateType=DELIVERY_ORDER&aggregateId=${smsOrder.id}`)).status())
      .toBe(403);
    expect((await api.get(`/api/v1/notification-customer-preferences/${customerId}`)).status()).toBe(403);
    expect((await api.put(`/api/v1/notification-customer-preferences/${customerId}`, { data: {
      emailEnabled: true, smsEnabled: false, version: null,
    } })).status()).toBe(403);
    await api.dispose();
  });

  function authorized(auth: Auth) {
    return request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } });
  }

  async function createReadyOrder(api: Awaited<ReturnType<typeof request.newContext>>,
                                  originId: string, destinationId: string) {
    const now = Date.now();
    const response = await api.post('/api/v1/deliveries', { data: { customerId, originLocationId: originId,
      destinationLocationId: destinationId, priority: 'NORMAL', serviceType: 'STANDARD',
      windowStart: new Date(now - 60_000).toISOString(), windowEnd: new Date(now + 7_200_000).toISOString() } });
    expect(response.status(), await response.text()).toBe(201);
    const order = await response.json() as Order;
    const ready = await api.post(`/api/v1/deliveries/${order.id}/validate-readiness`, { data: { version: order.version } });
    expect(ready.status(), await ready.text()).toBe(200);
    return await ready.json() as Order;
  }

  async function completeOrder(api: Awaited<ReturnType<typeof request.newContext>>, order: Order) {
    const created = await api.post(`/api/v1/deliveries/${order.id}/proof`, { data: { deliveryVersion: order.version } });
    expect(created.status(), await created.text()).toBe(201);
    const proof = await created.json() as Proof;
    const evidence = await api.post(`/api/v1/deliveries/${order.id}/proof/evidence`, { multipart: {
      podVersion: String(proof.version), type: 'BARCODE', barcodeValue: order.deliveryNumber,
      captureSource: 'SCANNER',
    } });
    expect(evidence.status(), await evidence.text()).toBe(201);
    const withEvidence = await evidence.json() as Proof;
    const finalized = await api.post(`/api/v1/deliveries/${order.id}/proof/finalize`, { data: {
      deliveryVersion: order.version, podVersion: withEvidence.version,
    } });
    expect(finalized.status(), await finalized.text()).toBe(200);
  }

  async function deliveryHistory(api: Awaited<ReturnType<typeof request.newContext>>, orderId: string) {
    const response = await api.get('/api/v1/notification-deliveries', { params: {
      aggregateType: 'DELIVERY_ORDER', aggregateId: orderId, limit: 200,
    } });
    expect(response.status(), await response.text()).toBe(200);
    return await response.json() as Notification[];
  }
});
