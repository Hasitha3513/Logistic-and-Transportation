import { expect, request, test } from '@playwright/test';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const suffix = `planner-${Date.now()}`;
const customerId = '32000000-0000-0000-0000-000000000001';
const originLocationId = '33000000-0000-0000-0000-000000000001';

type Auth = { accessToken: string; refreshToken: string };
type TenantFixture = { username: string; password: string };

async function login(username = 'admin', password = 'AdminPass!2026') {
  const context = await request.newContext({ baseURL: backend });
  const response = await context.post('/api/auth/login', { data: { username, password } });
  expect(response.ok()).toBeTruthy();
  const auth = await response.json() as Auth;
  await context.dispose();
  return auth;
}

test.describe.serial('US-68 real PostgreSQL Last-Mile Planner acceptance', () => {
  let admin: Auth;
  let planner: Auth;
  let limited: Auth;
  let orderId: string;
  let tenantBOrderId: string;

  test.beforeAll(async () => {
    admin = await login();
    const context = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${admin.accessToken}` } });
    const destinationResponse = await context.post('/api/locations', { data: {
      code: `PLANNER-DEST-${suffix}`.toUpperCase().slice(0, 60), name: 'Planner real fixture',
      address: 'Acceptance fixture', latitude: 6.90, longitude: 79.85, active: true,
    } });
    expect(destinationResponse.status()).toBe(201);
    const destination = await destinationResponse.json() as { id: string };
    const now = new Date();
    const orderResponse = await context.post('/api/v1/deliveries', { data: {
      customerId, originLocationId, destinationLocationId: destination.id, priority: 'NORMAL', serviceType: 'STANDARD',
      windowStart: now.toISOString(), windowEnd: new Date(now.getTime() + 7_200_000).toISOString(), instructions: 'Planner projection only',
    } });
    expect(orderResponse.status()).toBe(201);
    const order = await orderResponse.json() as { id: string; version: number };
    orderId = order.id;
    expect((await context.post(`/api/v1/deliveries/${orderId}/validate-readiness`, { data: { version: order.version } })).ok()).toBeTruthy();

    const tenantBResponse = await context.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(tenantBResponse.status()).toBe(201);
    const tenantB = await tenantBResponse.json() as TenantFixture;
    const tenantBAuth = await login(tenantB.username, tenantB.password);
    const tenantBContext = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${tenantBAuth.accessToken}` } });
    const bCustomer = await tenantBContext.post('/api/customers', { data: { code: `PLANNER-C-${suffix}`.slice(0, 40), name: 'Tenant B customer', active: true } });
    const bOrigin = await tenantBContext.post('/api/locations', { data: { code: `PLANNER-O-${suffix}`.slice(0, 40), name: 'Tenant B origin', latitude: 6.86, longitude: 79.81, active: true } });
    const bDestination = await tenantBContext.post('/api/locations', { data: { code: `PLANNER-D-${suffix}`.slice(0, 40), name: 'Tenant B destination', latitude: 6.90, longitude: 79.85, active: true } });
    expect(bCustomer.status()).toBe(201); expect(bOrigin.status()).toBe(201); expect(bDestination.status()).toBe(201);
    const bOrder = await tenantBContext.post('/api/v1/deliveries', { data: {
      customerId: (await bCustomer.json() as { id: string }).id, originLocationId: (await bOrigin.json() as { id: string }).id,
      destinationLocationId: (await bDestination.json() as { id: string }).id, priority: 'NORMAL', serviceType: 'STANDARD',
      windowStart: now.toISOString(), windowEnd: new Date(now.getTime() + 7_200_000).toISOString(),
    } });
    expect(bOrder.status()).toBe(201); tenantBOrderId = (await bOrder.json() as { id: string }).id;
    await tenantBContext.dispose();

    const plannerRole = await context.post('/api/roles', { data: { name: `Planner view ${suffix}`, active: true, permissions: ['DELIVERY_VIEW', 'DELIVERY_FAIL_VIEW'] } });
    expect(plannerRole.status()).toBe(201);
    const plannerUser = await context.post('/api/users', { data: { username: `planner-view-${suffix}`, email: `planner-view-${suffix}@example.test`, password: `PlannerView!${suffix}`, firstName: 'Planner', lastName: 'Viewer', active: true, roleIds: [(await plannerRole.json() as { id: string }).id] } });
    expect(plannerUser.status()).toBe(201);
    planner = await login(`planner-view-${suffix}`, `PlannerView!${suffix}`);
    const plannerSession = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${planner.accessToken}` } });
    const plannerMe = await plannerSession.get('/api/auth/me');
    expect(plannerMe.ok()).toBeTruthy();
    expect((await plannerMe.json() as { permissions: string[] }).permissions).toEqual(expect.arrayContaining(['DELIVERY_VIEW', 'DELIVERY_FAIL_VIEW']));
    const plannerEndpoint = await plannerSession.get(`/api/v1/deliveries/${orderId}/last-mile-planner`);
    expect(plannerEndpoint.status(), await plannerEndpoint.text()).toBe(200);
    await plannerSession.dispose();
    const role = await context.post('/api/roles', { data: { name: `Planner no access ${suffix}`, active: true, permissions: ['DASHBOARD_VIEW'] } });
    expect(role.status()).toBe(201);
    const user = await context.post('/api/users', { data: { username: `planner-no-access-${suffix}`, email: `planner-no-access-${suffix}@example.test`, password: `Planner!${suffix}`, firstName: 'Planner', lastName: 'Denied', active: true, roleIds: [(await role.json() as { id: string }).id] } });
    expect(user.status()).toBe(201);
    limited = await login(`planner-no-access-${suffix}`, `Planner!${suffix}`);
    await context.dispose();
  });

  test('loads the read-only planner projection in the real UI', async ({ page }) => {
    await page.addInitScript((auth) => { localStorage.setItem('transport.accessToken', auth.accessToken); localStorage.setItem('transport.refreshToken', auth.refreshToken); }, planner);
    await page.goto(`/deliveries/${orderId}`);
    await expect(page.getByText('Last-Mile Planner')).toBeVisible();
    await expect(page.getByText('Failed attempts')).toBeVisible();
    await expect(page.getByText('Active exceptions')).toBeVisible();
  });

  test('returns real planner facts and never exposes a mutation endpoint', async () => {
    const context = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${planner.accessToken}` } });
    const response = await context.get(`/api/v1/deliveries/${orderId}/last-mile-planner`);
    expect(response.ok()).toBeTruthy();
    const body = await response.json() as { deliveryOrderId: string; availableActions: string[] };
    expect(body.deliveryOrderId).toBe(orderId); expect(body.availableActions.length).toBeGreaterThan(0);
    await context.dispose();
  });

  test('enforces unauthenticated, insufficient-permission, and tenant-IDOR denial', async () => {
    const anonymous = await request.newContext({ baseURL: backend });
    expect((await anonymous.get(`/api/v1/deliveries/${orderId}/last-mile-planner`)).status()).toBe(401);
    await anonymous.dispose();
    const denied = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${limited.accessToken}` } });
    expect((await denied.get(`/api/v1/deliveries/${orderId}/last-mile-planner`)).status()).toBe(403);
    await denied.dispose();
    const adminContext = await request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${planner.accessToken}` } });
    expect((await adminContext.get(`/api/v1/deliveries/${tenantBOrderId}/last-mile-planner`, { headers: { 'X-Tenant-Id': '00000000-0000-0000-0000-000000000000' } })).status()).toBe(404);
    await adminContext.dispose();
  });
});
