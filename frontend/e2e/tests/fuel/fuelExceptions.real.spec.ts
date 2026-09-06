import { expect, request, test } from '@playwright/test';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const suffix = `us38-${Date.now()}`;
type Auth = { accessToken: string; refreshToken: string };
type Api = Awaited<ReturnType<typeof request.newContext>>;
type Entity = { id: string };
type Tank = Entity & { currentStockLiters: number };
type FuelCase = Entity & { category: string; lifecycle: string; impact: string; sourceType: string;
  sourceId: string; version: number; handoffStatus: string; summary: string };

test.describe.serial('US-38 real PostgreSQL fuel-exception acceptance', () => {
  let admin: Auth; let approver: Auth; let limited: Auth; let tenantB: Auth; let tank: Tank;
  let vehicleId: string; let driverId: string;
  const cases: FuelCase[] = [];

  test.beforeAll(async () => {
    admin = await login(); const api = await authorized(admin);
    approver = await createUser(api, 'approver', ['FUEL_EXCEPTION_VIEW', 'FUEL_EXCEPTION_APPROVE']);
    limited = await createUser(api, 'limited', ['FUEL_EXCEPTION_VIEW']);
    const otherTenant = await api.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(otherTenant.status(), await otherTenant.text()).toBe(201);
    const other = await otherTenant.json() as { username: string; password: string };
    tenantB = await login(other.username, other.password);
    const stations = await json<Entity[]>(api.get('/api/fuel-stations'));
    expect(stations.length).toBeGreaterThan(0);
    const vehicles = entities(await json<unknown>(api.get('/api/vehicles')));
    const drivers = entities(await json<unknown>(api.get('/api/drivers')));
    expect(vehicles.length).toBeGreaterThan(0); expect(drivers.length).toBeGreaterThan(0);
    vehicleId = vehicles[0].id; driverId = drivers[0].id;
    const createdTank = await api.post('/api/bunker-tanks', { data: { fuelStationId: stations[0].id,
      tankCode: suffix.toUpperCase(), tankName: `US-38 acceptance ${suffix}`, fuelType: 'DIESEL',
      capacityLiters: 1000, minimumStockLiters: 10, openingBalanceLiters: 100 } });
    expect(createdTank.status(), await createdTank.text()).toBe(201); tank = await createdTank.json() as Tank; await api.dispose();
  });

  test('1/6 creates suspected-loss review evidence and uses safe language in Chromium', async ({ page }) => {
    const api = await authorized(admin);
    const value = await createCase(api, 'SUSPECTED_FUEL_LOSS', 'MEDIUM', 'Suspected unexplained consumption; review required');
    cases.push(value);
    const evidence = await api.post(`/api/v1/fuel/exceptions/${value.id}/evidence`, { data: {
      evidenceType: 'US37_INDICATOR', sourceType: 'BUNKER_TANK', sourceId: tank.id,
      summary: 'POSSIBLE_LEAKAGE_INDICATOR retained as review evidence', safeSnapshot: { indicator: 'POSSIBLE_LEAKAGE_INDICATOR' } } });
    expect(evidence.status(), await evidence.text()).toBe(201);
    await authenticatePage(page, admin); await page.goto('/fuel/exceptions');
    await expect(page.getByText('Fuel exceptions are review records')).toBeVisible();
    await page.getByText('SUSPECTED_FUEL_LOSS').first().click();
    await expect(page.getByText(value.summary, { exact: true }).first()).toBeVisible();
    const body = (await page.locator('body').innerText()).toUpperCase();
    for (const forbidden of ['THEFT_CONFIRMED', 'FRAUD_CONFIRMED', 'DRIVER_GUILTY']) expect(body).not.toContain(forbidden);
    await api.dispose();
  });

  test('2/6 applies an independently approved incorrect-reading owner correction', async () => {
    const requester = await authorized(admin); const reviewer = await authorized(approver);
    let value = await createCase(requester, 'INCORRECT_READING', 'HIGH', 'Incorrect reading requires compensating review');
    cases.push(value);
    value = await command<FuelCase>(requester, value, 'review', { reason: 'Verified source reading review' });
    const correctionResponse = await requester.post(`/api/v1/fuel/exceptions/${value.id}/corrections`, { data: {
      correctionType: 'BUNKER_STOCK_ADJUSTMENT', changesFinancialFact: true,
      ownerCommand: { tankId: tank.id, quantityDeltaLiters: '0.001', reason: 'US-38 compensating correction' } } });
    expect(correctionResponse.status(), await correctionResponse.text()).toBe(201);
    const correction = await correctionResponse.json() as { id: string; version: number };
    const selfApproval = await requester.post(`/api/v1/fuel/exceptions/${value.id}/corrections/${correction.id}/approve`,
      { data: { version: correction.version, reason: 'Self approval must fail' } });
    expect(selfApproval.status()).toBe(400);
    const approved = await reviewer.post(`/api/v1/fuel/exceptions/${value.id}/corrections/${correction.id}/approve`,
      { data: { version: correction.version, reason: 'Independent compensating correction approval' } });
    expect(approved.status(), await approved.text()).toBe(200);
    const applied = await approved.json() as { status: string; ownerResultReference?: string };
    expect(applied.status).toBe('APPLIED'); expect(applied.ownerResultReference).toBeTruthy();
    await requester.dispose(); await reviewer.dispose();
  });

  test('3/6 records sudden-price and emergency-refuel cases without rewriting source facts', async () => {
    const api = await authorized(admin);
    const before = await json<Tank>(api.get(`/api/bunker-tanks/${tank.id}`));
    cases.push(await createCase(api, 'SUDDEN_PRICE_CHANGE', 'HIGH', 'Sudden price change requires effective-dated review'));
    cases.push(await createCase(api, 'EMERGENCY_REFUEL', 'HIGH', 'Emergency refuel recorded retrospectively for review', {
      vehicleId, driverId, occurredAt: new Date().toISOString() }));
    const missing = await api.post('/api/v1/fuel/exceptions', { data: { category: 'EMERGENCY_REFUEL', impact: 'HIGH',
      sourceType: 'BUNKER_TANK', sourceId: tank.id, summary: `Missing emergency reference ${suffix}`,
      vehicleId: crypto.randomUUID(), driverId: crypto.randomUUID(), occurredAt: new Date().toISOString() } });
    expect(missing.status()).toBe(404);
    const after = await json<Tank>(api.get(`/api/bunker-tanks/${tank.id}`));
    expect(after.currentStockLiters).toBe(before.currentStockLiters); await api.dispose();
  });

  test('4/6 records a fuel-card policy deviation while preserving US-35 command separation', async () => {
    const api = await authorized(admin);
    cases.push(await createCase(api, 'FUEL_CARD_POLICY_DEVIATION', 'MEDIUM', 'Fuel-card policy deviation requires independent review'));
    const forbidden = await api.patch(`/api/v1/fuel/card-transactions/${crypto.randomUUID()}`, { data: { localStatus: 'RECONCILED' } });
    expect([403, 405]).toContain(forbidden.status()); await api.dispose();
  });

  test('5/6 rejects negative bunker stock, keeps stock and ledger unchanged, and creates one case', async () => {
    const api = await authorized(admin);
    const before = await json<Tank>(api.get(`/api/bunker-tanks/${tank.id}`));
    const movementsBefore = await json<{ totalElements: number }>(api.get(`/api/bunker-tanks/${tank.id}/movements?limit=1`));
    const rejected = await api.post(`/api/bunker-tanks/${tank.id}/adjustments`, { data: {
      quantityDeltaLiters: String(-(Number(before.currentStockLiters) + 1)), reason: 'US-38 negative balance rejection' } });
    expect(rejected.status()).toBe(400);
    const after = await json<Tank>(api.get(`/api/bunker-tanks/${tank.id}`));
    const movementsAfter = await json<{ totalElements: number }>(api.get(`/api/bunker-tanks/${tank.id}/movements?limit=1`));
    expect(after.currentStockLiters).toBe(before.currentStockLiters);
    expect(movementsAfter.totalElements).toBe(movementsBefore.totalElements);
    const found = await json<FuelCase[]>(api.get(`/api/v1/fuel/exceptions?category=NEGATIVE_BUNKER_BALANCE&tankId=${tank.id}`));
    expect(found).toHaveLength(1);
    const replay = await api.post(`/api/bunker-tanks/${tank.id}/adjustments`, { data: {
      quantityDeltaLiters: String(-(Number(before.currentStockLiters) + 1)), reason: 'US-38 negative balance rejection' } });
    expect(replay.status()).toBe(400);
    const replayed = await json<FuelCase[]>(api.get(`/api/v1/fuel/exceptions?category=NEGATIVE_BUNKER_BALANCE&tankId=${tank.id}`));
    expect(replayed).toHaveLength(1);
    cases.push(found[0]); await api.dispose();
  });

  test('6/6 performs durable critical handoff and enforces tenant and command RBAC', async () => {
    const api = await authorized(admin);
    let critical = await createCase(api, 'SUSPECTED_FUEL_LOSS', 'CRITICAL',
      'Critical unexplained variance requiring Operations coordination', { sourceType: 'REJECTED_BUNKER_COMMAND' });
    critical = await command<FuelCase>(api, critical, 'escalate', { reason: '[E2E_FAIL_FIRST] Cross-module coordination required' });
    expect(critical.handoffStatus).toBe('FAILED');
    critical = await command<FuelCase>(api, critical, 'escalate', { reason: 'Retry durable Operations handoff' });
    expect(critical.handoffStatus).toBe('PUBLISHED');
    await expect.poll(async () => {
      const operations = await json<{ content: Array<{ sourceModule: string; sourceId: string }> }>(
        api.get('/api/v1/operational-exceptions?sourceModule=FUEL'));
      return operations.content.filter(value => value.sourceId === critical.id).length;
    }, { timeout: 15_000 }).toBe(1);
    const other = await authorized(tenantB); expect((await other.get(`/api/v1/fuel/exceptions/${critical.id}`)).status()).toBe(404);
    const viewer = await authorized(limited);
    for (const call of [
      viewer.post('/api/v1/fuel/exceptions', { data: {} }),
      viewer.post(`/api/v1/fuel/exceptions/${critical.id}/review`, { data: { version: critical.version, reason: 'denied' } }),
      viewer.post(`/api/v1/fuel/exceptions/${critical.id}/corrections`, { data: {} }),
      viewer.post(`/api/v1/fuel/exceptions/${critical.id}/escalate`, { data: { version: critical.version, reason: 'denied' } })]) {
      expect((await call).status()).toBe(403);
    }
    const note2000 = await api.post(`/api/v1/fuel/exceptions/${cases[0].id}/notes`, { data: { text: 'n'.repeat(2000) } });
    expect(note2000.status(), await note2000.text()).toBe(201);
    const note2001 = await api.post(`/api/v1/fuel/exceptions/${cases[0].id}/notes`, { data: { text: 'n'.repeat(2001) } });
    expect(note2001.status()).toBe(400);
    const evidenceDetail = await json<{ evidence: unknown[] }>(api.get(`/api/v1/fuel/exceptions/${cases[0].id}`));
    const handoffDetail = await json<{ history: Array<{ action: string }> }>(api.get(`/api/v1/fuel/exceptions/${critical.id}`));
    expect(evidenceDetail.evidence.length).toBeGreaterThan(0); expect(handoffDetail.history.length).toBeGreaterThan(0);
    expect(handoffDetail.history.map(value => value.action)).toEqual(expect.arrayContaining([
      'HANDOFF_CREATED', 'HANDOFF_FAILED', 'HANDOFF_RETRIED', 'HANDOFF_PUBLISHED']));
    await api.dispose(); await other.dispose(); await viewer.dispose();
  });

  async function createCase(api: Api, category: string, impact: string, summary: string, extra: Record<string, unknown> = {}) {
    const response = await api.post('/api/v1/fuel/exceptions', { data: { category, impact, sourceType: 'BUNKER_TANK',
      sourceId: tank.id, summary: `${summary} ${suffix}`, safeMetadata: { origin: 'real-e2e' }, ...extra } });
    expect(response.status(), await response.text()).toBe(201); return response.json() as Promise<FuelCase>;
  }
  async function command<T>(api: Api, value: FuelCase, action: string, data: Record<string, unknown>) {
    const response = await api.post(`/api/v1/fuel/exceptions/${value.id}/${action}`, { data: { version: value.version, ...data } });
    expect(response.status(), await response.text()).toBe(200); return response.json() as Promise<T>;
  }
  function authorized(auth: Auth) { return request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } }); }
  async function createUser(api: Api, label: string, permissions: string[]) {
    const role = await api.post('/api/roles', { data: { name: `US38 ${label} ${suffix}`, active: true, permissions } });
    expect(role.status(), await role.text()).toBe(201); const roleId = (await role.json() as Entity).id;
    const username = `us38-${label}-${suffix}`; const password = `Us38!${label}-${suffix}`;
    const user = await api.post('/api/users', { data: { username, email: `${username}@example.test`, password,
      firstName: 'US38', lastName: label, active: true, roleIds: [roleId] } });
    expect(user.status(), await user.text()).toBe(201); return login(username, password);
  }
});

async function json<T>(call: Promise<import('@playwright/test').APIResponse>): Promise<T> {
  const response = await call; expect(response.status(), await response.text()).toBe(200); return response.json() as Promise<T>;
}
async function authenticatePage(page: import('@playwright/test').Page, auth: Auth) { await page.addInitScript(value => {
  localStorage.setItem('transport.accessToken', value.accessToken); localStorage.setItem('transport.refreshToken', value.refreshToken); }, auth); }
async function login(username = process.env.E2E_ADMIN_USERNAME ?? 'admin', password = process.env.E2E_ADMIN_PASSWORD ?? 'AdminPass!2026') {
  const api = await request.newContext({ baseURL: backend }); const response = await api.post('/api/auth/login', { data: { username, password } });
  expect(response.status(), await response.text()).toBe(200); const auth = await response.json() as Auth; await api.dispose(); return auth;
}
function entities(value: unknown): Entity[] {
  if (Array.isArray(value)) return value as Entity[];
  return ((value as { content?: Entity[] }).content ?? []);
}
