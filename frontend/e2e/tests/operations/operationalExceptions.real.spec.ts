import { expect, request, test } from '@playwright/test';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const suffix = `us78-${Date.now()}`;
const originLocationId = '33000000-0000-0000-0000-000000000001';
type Auth = { accessToken: string; refreshToken: string };
type Api = Awaited<ReturnType<typeof request.newContext>>;
type Case = { id: string; caseReference: string; sourceModule: 'ROUTING' | 'DELIVERY'; sourceId: string;
  severity: string; status: string; escalationLevel: string; assignedRoleCode?: string; version: number };
type Detail = { exceptionCase: Case; correctiveActions: Array<{ id: string; status: string; version: number }>;
  rca?: { id: string; version: number; approverId?: string } };
type Page<T> = { content: T[]; totalElements: number };

async function login(username = process.env.E2E_ADMIN_USERNAME ?? 'admin',
                     password = process.env.E2E_ADMIN_PASSWORD ?? 'AdminPass!2026') {
  const api = await request.newContext({ baseURL: backend });
  const response = await api.post('/api/auth/login', { data: { username, password } });
  expect(response.status(), await response.text()).toBe(200);
  const auth = await response.json() as Auth; await api.dispose(); return auth;
}

test.describe.serial('US-78 real PostgreSQL cross-domain operational exception acceptance', () => {
  let admin: Auth; let approver: Auth; let closer: Auth; let limited: Auth; let tenantB: Auth;
  let deliveryReporter: Auth;
  let routeId: string; let disruptionId: string; let deliveryId: string; let deliveryExceptionId: string;
  let routingCase: Detail; let deliveryCase: Detail;

  test.beforeAll(async () => {
    admin = await login(); const api = await authorized(admin);
    approver = await createUser(api, 'approver', ['OPERATIONAL_EXCEPTION_VIEW', 'OPERATIONAL_EXCEPTION_RCA']);
    closer = await createUser(api, 'closer', ['OPERATIONAL_EXCEPTION_VIEW', 'OPERATIONAL_EXCEPTION_CLOSE']);
    limited = await createUser(api, 'viewer', ['OPERATIONAL_EXCEPTION_VIEW']);
    deliveryReporter = await createUser(api, 'delivery-reporter',
      ['DELIVERY_EXCEPTION_CREATE', 'DELIVERY_EXCEPTION_VIEW']);
    const other = await api.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(other.status(), await other.text()).toBe(201);
    const fixture = await other.json() as { username: string; password: string };
    tenantB = await login(fixture.username, fixture.password); await api.dispose();
  });

  test('1/6 creates real Routing and Delivery facts and renders exactly one case for each source', async ({ page }) => {
    const api = await authorized(admin);
    const destination = await createLocation(api, `US78-D-${suffix}`);
    const route = await api.post('/api/routes', { data: { code: `US78-${Date.now()}`.slice(0, 40),
      name: 'US-78 real routing source', originLocationId, destinationLocationId: destination,
      plannedDistanceKm: 18.5, estimatedDurationMinutes: 45, stops: [], active: true } });
    expect(route.status(), await route.text()).toBe(201); routeId = (await route.json() as { id: string }).id;
    const disruption = await api.post(`/api/routes/${routeId}/disruptions`, { data: {
      disruptionType: 'ROAD_CLOSURE', severity: 'HIGH', description: 'US-78 controlled road closure',
      effectiveFrom: new Date().toISOString() } });
    expect(disruption.status(), await disruption.text()).toBe(201);
    disruptionId = (await disruption.json() as { id: string }).id;

    const order = await api.post('/api/e2e/delivery-exception-fixtures');
    expect(order.status(), await order.text()).toBe(200); deliveryId = (await order.json() as { id: string }).id;
    const deliveryApi = await authorized(deliveryReporter);
    const exception = await deliveryApi.post(`/api/v1/deliveries/${deliveryId}/exceptions`, { data: {
      exceptionType: 'WRONG_ADDRESS', severity: 'HIGH', description: 'US-78 controlled address exception',
      evidenceList: [] } });
    expect(exception.status(), await exception.text()).toBe(201);
    deliveryExceptionId = (await exception.json() as { id: string }).id;
    await process(api); const cases = await list(api);
    const routeMatches = cases.filter(item => item.sourceModule === 'ROUTING' && item.sourceId === disruptionId);
    const deliveryMatches = cases.filter(item => item.sourceModule === 'DELIVERY' && item.sourceId === deliveryExceptionId);
    expect(routeMatches).toHaveLength(1); expect(deliveryMatches).toHaveLength(1);
    routingCase = await get(api, routeMatches[0].id); deliveryCase = await get(api, deliveryMatches[0].id);
    await deliveryApi.dispose(); await authenticatePage(page, admin); await page.goto('/operations/exceptions');
    await expect(page.getByText('ROUTING · ROUTE_DISRUPTION_CREATED').first()).toBeVisible();
    await expect(page.getByText('DELIVERY · DELIVERY_EXCEPTION_CREATED').first()).toBeVisible(); await api.dispose();
  });

  test('2/6 deduplicates exact durable replays and hides both IDs from Tenant B', async () => {
    const api = await authorized(admin);
    for (const item of [routingCase, deliveryCase]) {
      expect((await api.post(`/api/e2e/operational-exceptions/${item.exceptionCase.id}/replay`)).status()).toBe(200);
    }
    await process(api);
    for (const item of [routingCase, deliveryCase]) {
      const evidence: { caseCount: number; intakeOutboxCount: number } = await (await api.get(
        `/api/e2e/operational-exceptions/${item.exceptionCase.id}/evidence`)).json();
      expect(evidence.caseCount).toBe(1); expect(evidence.intakeOutboxCount).toBe(1);
    }
    const other = await authorized(tenantB); const others = await list(other);
    expect(others.some(item => [routingCase.exceptionCase.id, deliveryCase.exceptionCase.id].includes(item.id))).toBe(false);
    expect((await other.get(`/api/v1/operational-exceptions/${routingCase.exceptionCase.id}`)).status()).toBe(404);
    await api.dispose(); await other.dispose();
  });

  test('3/6 acknowledges, assigns, and starts both source domains through the common lifecycle', async () => {
    const api = await authorized(admin);
    for (const key of ['routingCase', 'deliveryCase'] as const) {
      let detail = key === 'routingCase' ? routingCase : deliveryCase;
      detail = await command(api, detail, 'acknowledge', { expectedVersion: detail.exceptionCase.version });
      detail = await command(api, detail, 'assign', { expectedVersion: detail.exceptionCase.version,
        assignmentType: 'ROLE_QUEUE', roleCode: 'OPERATIONS_QUEUE', reason: 'Common queue ownership' });
      detail = await command(api, detail, 'start', { expectedVersion: detail.exceptionCase.version });
      expect(detail.exceptionCase.status).toBe('IN_PROGRESS');
      expect(detail.exceptionCase.assignedRoleCode).toBe('OPERATIONS_QUEUE');
      if (key === 'routingCase') routingCase = detail; else deliveryCase = detail;
    }
    await api.dispose();
  });

  test('4/6 completes corrective actions, escalates safely, and delivers the bounded Notification fact', async () => {
    const api = await authorized(admin);
    for (const key of ['routingCase', 'deliveryCase'] as const) {
      let detail = key === 'routingCase' ? routingCase : deliveryCase;
      detail = await command(api, detail, 'corrective-actions', { expectedVersion: detail.exceptionCase.version,
        type: 'CORRECTIVE', description: 'Apply controlled source remediation', ownerType: 'ROLE_QUEUE',
        ownerRoleCode: 'OPERATIONS_QUEUE', evidenceReference: 'US78-E2E-EVIDENCE' });
      let action = detail.correctiveActions.at(-1)!;
      detail = await nestedCommand(api, detail, `corrective-actions/${action.id}/start`, { expectedVersion: action.version });
      action = detail.correctiveActions.find(candidate => candidate.id === action.id)!;
      detail = await nestedCommand(api, detail, `corrective-actions/${action.id}/complete`, { expectedVersion: action.version });
      detail = await command(api, detail, 'escalate', { expectedVersion: detail.exceptionCase.version,
        reason: 'Controlled high severity escalation' });
      expect(detail.exceptionCase.escalationLevel).toBe('L1');
      if (key === 'routingCase') routingCase = detail; else deliveryCase = detail;
    }
    await process(api);
    const evidence: { notification: { status: string; payload: string } } = await (await api.get(
      `/api/e2e/operational-exceptions/${routingCase.exceptionCase.id}/evidence`)).json();
    expect(evidence.notification.status).toBe('PUBLISHED');
    expect(evidence.notification.payload).toContain('caseReference');
    expect(evidence.notification.payload).not.toMatch(/description|resolutionNote|actorUsername/i); await api.dispose();
  });

  test('5/6 enforces separate RCA approver and closer while preserving the full timeline', async () => {
    const author = await authorized(admin); const approvalApi = await authorized(approver);
    const closeApi = await authorized(closer);
    for (const key of ['routingCase', 'deliveryCase'] as const) {
      let detail = key === 'routingCase' ? routingCase : deliveryCase;
      detail = await command(author, detail, 'rca', { expectedVersion: detail.exceptionCase.version,
        causeCategory: 'PROCESS', rootCauseCode: 'US78_CONTROL_GAP', summary: 'Controlled process gap',
        contributingFactors: 'Acceptance test fixture' });
      const approved = await approvalApi.post(`/api/v1/operational-exceptions/${detail.exceptionCase.id}/rca/approve`,
        { data: { expectedCaseVersion: detail.exceptionCase.version, expectedRcaVersion: detail.rca!.version } });
      expect(approved.status(), await approved.text()).toBe(200); detail = await approved.json() as Detail;
      detail = await command(author, detail, 'resolve', { expectedVersion: detail.exceptionCase.version,
        resolutionNote: 'Controlled remediation validated', resultReference: `${detail.exceptionCase.sourceModule}:UNCHANGED` });
      const closed = await closeApi.post(`/api/v1/operational-exceptions/${detail.exceptionCase.id}/close`,
        { data: { expectedVersion: detail.exceptionCase.version } });
      expect(closed.status(), await closed.text()).toBe(200); detail = await closed.json() as Detail;
      expect(detail.exceptionCase.status).toBe('CLOSED');
      const history = await author.get(`/api/v1/operational-exceptions/${detail.exceptionCase.id}/history?size=100`);
      expect(history.status(), await history.text()).toBe(200);
      expect(((await history.json()) as Page<unknown>).content.length).toBeGreaterThanOrEqual(9);
      if (key === 'routingCase') routingCase = detail; else deliveryCase = detail;
    }
    await author.dispose(); await approvalApi.dispose(); await closeApi.dispose();
  });

  test('6/6 leaves Routing and Delivery sources unchanged and enforces command RBAC', async () => {
    const api = await authorized(admin);
    const disruptions = await api.get(`/api/routes/${routeId}/disruptions`);
    expect(disruptions.status(), await disruptions.text()).toBe(200);
    expect((await disruptions.json() as Array<{ id: string; status: string }>)).toContainEqual(
      expect.objectContaining({ id: disruptionId, status: 'ACTIVE' }));
    const deliveryApi = await authorized(deliveryReporter);
    const exceptions = await deliveryApi.get(`/api/v1/deliveries/${deliveryId}/exceptions`);
    expect(exceptions.status(), await exceptions.text()).toBe(200);
    expect((await exceptions.json() as Array<{ id: string; status: string }>)).toContainEqual(
      expect.objectContaining({ id: deliveryExceptionId, status: 'OPEN' }));
    const viewer = await authorized(limited);
    expect((await viewer.get('/api/v1/operational-exceptions')).status()).toBe(200);
    expect((await viewer.post(`/api/v1/operational-exceptions/${routingCase.exceptionCase.id}/reopen`,
      { data: { expectedVersion: routingCase.exceptionCase.version, reason: 'must be denied' } })).status()).toBe(403);
    await api.dispose(); await deliveryApi.dispose(); await viewer.dispose();
  });

  function authorized(auth: Auth) { return request.newContext({ baseURL: backend,
    extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } }); }
  async function createUser(api: Api, label: string, permissions: string[]) {
    const role = await api.post('/api/roles', { data: { name: `US78 ${label} ${suffix}`, active: true, permissions } });
    expect(role.status(), await role.text()).toBe(201); const roleId = (await role.json() as { id: string }).id;
    const username = `us78-${label}-${suffix}`; const password = `Us78!${label}-${suffix}`;
    const user = await api.post('/api/users', { data: { username, email: `${username}@example.test`, password,
      firstName: 'US78', lastName: label, active: true, roleIds: [roleId] } });
    expect(user.status(), await user.text()).toBe(201); return login(username, password);
  }
  async function createLocation(api: Api, code: string) {
    const response = await api.post('/api/locations', { data: { code: code.slice(0, 40), name: 'US-78 destination',
      address: 'Acceptance fixture', latitude: 6.91, longitude: 79.86, active: true } });
    expect(response.status(), await response.text()).toBe(201); return (await response.json() as { id: string }).id;
  }
  async function list(api: Api) {
    const response = await api.get('/api/v1/operational-exceptions?size=100');
    expect(response.status(), await response.text()).toBe(200); return ((await response.json()) as Page<Case>).content;
  }
  async function get(api: Api, id: string) {
    const response = await api.get(`/api/v1/operational-exceptions/${id}`);
    expect(response.status(), await response.text()).toBe(200); return await response.json() as Detail;
  }
  async function process(api: Api) {
    const response = await api.post('/api/e2e/operational-exceptions/process');
    expect(response.status(), await response.text()).toBe(200);
  }
  async function command(api: Api, detail: Detail, action: string, data: object) {
    const response = await api.post(`/api/v1/operational-exceptions/${detail.exceptionCase.id}/${action}`, { data });
    expect(response.status(), await response.text()).toBe(200); return await response.json() as Detail;
  }
  async function nestedCommand(api: Api, detail: Detail, action: string, data: object) {
    return command(api, detail, action, data);
  }
  async function authenticatePage(page: import('@playwright/test').Page, auth: Auth) {
    await page.addInitScript(value => { localStorage.setItem('transport.accessToken', value.accessToken);
      localStorage.setItem('transport.refreshToken', value.refreshToken); }, auth);
  }
});
