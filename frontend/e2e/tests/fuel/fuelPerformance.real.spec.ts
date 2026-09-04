import { expect, request, test } from '@playwright/test';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const suffix = `us37-${Date.now()}`;
type Auth = { accessToken: string; refreshToken: string };
type Entity = { id: string };
type Page<T> = { content: T[]; totalElements: number };

async function login(username = process.env.E2E_ADMIN_USERNAME ?? 'admin',
                     password = process.env.E2E_ADMIN_PASSWORD ?? 'AdminPass!2026') {
  const api = await request.newContext({ baseURL: backend });
  const response = await api.post('/api/auth/login', { data: { username, password } });
  expect(response.status(), await response.text()).toBe(200);
  const auth = await response.json() as Auth;
  await api.dispose();
  return auth;
}

test.describe.serial('US-37 real PostgreSQL fuel performance acceptance', () => {
  let admin: Auth;
  let tenantB: Auth;
  let vehicleIds: string[];
  let driverIds: string[];
  let issueIds: string[];
  let fuelType: string;
  let sourceBefore: unknown[];

  test.beforeAll(async () => {
    admin = await login();
    const api = await authorized(admin);
    const [stationsResponse, meResponse, categoriesResponse, typesResponse] = await Promise.all([
      api.get('/api/fuel-stations'), api.get('/api/auth/me'),
      api.get('/api/vehicle-categories'), api.get('/api/vehicle-types'),
    ]);
    for (const response of [stationsResponse, meResponse, categoriesResponse, typesResponse]) {
      expect(response.status(), await response.text()).toBe(200);
    }
    const categories = entities(await categoriesResponse.json());
    const types = entities(await typesResponse.json());
    expect(categories.length).toBeGreaterThan(0);
    expect(types.length).toBeGreaterThan(0);
    vehicleIds = [];
    driverIds = [];
    for (let index = 0; index < 3; index++) {
      const vehicle = await api.post('/api/vehicles', { data: {
        registrationNumber: `US37-${suffix}-${index}`.toUpperCase(),
        categoryId: categories[0].id, typeId: types[0].id,
        chassisNumber: `US37-CH-${suffix}-${index}`.toUpperCase(),
        engineNumber: `US37-EN-${suffix}-${index}`.toUpperCase(),
        manufacturer: 'Acceptance', model: 'US37', manufactureYear: 2026,
        ownershipType: 'COMPANY_OWNED', operationalStatus: 'AVAILABLE', active: true,
      } });
      expect(vehicle.status(), await vehicle.text()).toBe(201);
      vehicleIds.push((await vehicle.json() as Entity).id);
      const driver = await api.post('/api/drivers', { data: {
        employeeNumber: `US37-${suffix}-${index}`.toUpperCase(), firstName: 'US37',
        lastName: `Driver ${index + 1}`, status: 'AVAILABLE', active: true,
      } });
      expect(driver.status(), await driver.text()).toBe(201);
      driverIds.push((await driver.json() as Entity).id);
    }
    const stations = entities(await stationsResponse.json());
    const me = await meResponse.json() as { id: string; permissions: string[] };
    expect(vehicleIds).toHaveLength(3);
    expect(driverIds).toHaveLength(3);
    expect(stations.length).toBeGreaterThan(0);
    expect(me.permissions).toContain('FUEL_PERFORMANCE_VIEW');
    const fixture = await api.post('/api/e2e/fuel-performance-fixtures', { data: {
      suffix, stationId: stations[0].id, actorId: me.id, vehicleIds, driverIds,
    } });
    expect(fixture.status(), await fixture.text()).toBe(201);
    const fixtureBody = await fixture.json() as { issueIds: string[]; fuelType: string };
    issueIds = fixtureBody.issueIds;
    fuelType = fixtureBody.fuelType;
    expect(issueIds.length).toBeGreaterThanOrEqual(35);
    sourceBefore = await sourceFacts(api);
    const tenant = await api.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(tenant.status(), await tenant.text()).toBe(201);
    const other = await tenant.json() as { username: string; password: string };
    tenantB = await login(other.username, other.password);
    await api.dispose();
  });

  test('1/6 displays real summary data and tenant-timezone period controls', async ({ page }) => {
    const api = await authorized(admin);
    const response = await api.get(`/api/v1/fuel/performance/summary?preset=7&measurementMode=DISTANCE&fuelType=${fuelType}`);
    expect(response.status(), await response.text()).toBe(200);
    const summary = await response.json() as { vehicleCount: number; driverCount: number; period: { timeZone: string } };
    expect(summary.vehicleCount).toBeGreaterThanOrEqual(3);
    expect(summary.driverCount).toBeGreaterThanOrEqual(3);
    expect(summary.period.timeZone).toBeTruthy();
    await authenticatePage(page, admin);
    await page.goto('/fuel/performance');
    await expect(page.getByText('Vehicle comparison')).toBeVisible();
    await expect(page.getByText(/Period .*\(/)).toBeVisible();
    await api.dispose();
  });

  test('2/6 applies period and both measurement modes to committed facts', async () => {
    const api = await authorized(admin);
    for (const mode of ['DISTANCE', 'ENGINE_HOURS']) {
      const response = await api.get(`/api/v1/fuel/performance/summary?preset=7&measurementMode=${mode}&fuelType=${fuelType}`);
      expect(response.status(), await response.text()).toBe(200);
      const body = await response.json() as { metrics: { consumptionRate: number | null; sampleCount: number } };
      expect(body.metrics.sampleCount).toBeGreaterThan(0);
      expect(body.metrics.consumptionRate, `${mode}: ${JSON.stringify(body)}`).not.toBeNull();
    }
    await api.dispose();
  });

  test('3/6 exposes pageable vehicle comparison and safe direct detail', async () => {
    const api = await authorized(admin);
    const list = await api.get(`/api/v1/fuel/performance/vehicles?preset=7&measurementMode=DISTANCE&size=2&fuelType=${fuelType}`);
    expect(list.status(), await list.text()).toBe(200);
    const body = await list.json() as Page<{ vehicleId: string; metrics: { sampleCount: number } }>;
    expect(body.content).toHaveLength(2);
    const detail = await api.get(`/api/v1/fuel/performance/vehicles/${body.content[0].vehicleId}?preset=7&measurementMode=DISTANCE&fuelType=${fuelType}`);
    expect(detail.status(), await detail.text()).toBe(200);
    expect((await detail.json() as { metrics: { sampleCount: number } }).metrics.sampleCount).toBeGreaterThan(0);
    await api.dispose();
  });

  test('4/6 exposes privacy-minimized attributed driver analysis', async () => {
    const api = await authorized(admin);
    const response = await api.get(`/api/v1/fuel/performance/drivers?preset=7&measurementMode=DISTANCE&fuelType=${fuelType}`);
    expect(response.status(), await response.text()).toBe(200);
    const body = await response.json() as Page<Record<string, unknown>>;
    expect(body.content.length).toBeGreaterThanOrEqual(3);
    const serialized = JSON.stringify(body.content);
    for (const forbidden of ['email', 'phone', 'address', 'medical', 'drugTest', 'payroll', 'licence']) {
      expect(serialized.toLowerCase()).not.toContain(forbidden.toLowerCase());
    }
    await api.dispose();
  });

  test('5/6 shows deterministic review indicators and explicit insufficient data', async () => {
    const api = await authorized(admin);
    const trend = await api.get(`/api/v1/fuel/performance/trends?preset=7&measurementMode=DISTANCE&fuelType=${fuelType}`);
    expect(trend.status(), await trend.text()).toBe(200);
    const rows = await trend.json() as { indicators: string[] }[];
    expect(rows.some(row => row.indicators.includes('EFFICIENCY_DEVIATION'))).toBe(true);
    expect(rows.some(row => row.indicators.includes('POSSIBLE_LEAKAGE_INDICATOR'))).toBe(true);
    const insufficient = await api.get('/api/v1/fuel/performance/summary?preset=7&measurementMode=DISTANCE&fuelType=KEROSENE');
    expect(insufficient.status(), await insufficient.text()).toBe(200);
    expect((await insufficient.json() as { metrics: { quality: string; consumptionRate: number | null } }).metrics)
      .toMatchObject({ quality: 'INSUFFICIENT', consumptionRate: null });
    await api.dispose();
  });

  test('6/6 denies Tenant B and proves exact source immutability', async () => {
    const other = await authorized(tenantB);
    const foreign = await other.get(`/api/v1/fuel/performance/vehicles/${vehicleIds[0]}?preset=7`);
    expect(foreign.status()).toBe(404);
    const otherSummary = await other.get('/api/v1/fuel/performance/summary?preset=7');
    expect(otherSummary.status(), await otherSummary.text()).toBe(200);
    expect((await otherSummary.json() as { vehicleCount: number }).vehicleCount).toBe(0);
    const api = await authorized(admin);
    expect(await sourceFacts(api)).toEqual(sourceBefore);
    await other.dispose();
    await api.dispose();
  });

  function authorized(auth: Auth) {
    return request.newContext({ baseURL: backend,
      extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } });
  }

  function entities(value: unknown): Entity[] {
    return Array.isArray(value) ? value as Entity[] : (value as Page<Entity>).content;
  }

  async function sourceFacts(api: Awaited<ReturnType<typeof request.newContext>>) {
    const response = await api.get(`/api/fuel-issues?limit=100&voucherNumber=US37-${suffix}`);
    expect(response.status(), await response.text()).toBe(200);
    const body = await response.json() as Page<Record<string, unknown>>;
    return body.content.filter(item => issueIds.includes(String(item.id)))
      .sort((left, right) => String(left.id).localeCompare(String(right.id)));
  }

  async function authenticatePage(page: import('@playwright/test').Page, auth: Auth) {
    await page.addInitScript(value => {
      localStorage.setItem('transport.accessToken', value.accessToken);
      localStorage.setItem('transport.refreshToken', value.refreshToken);
    }, auth);
  }
});
