import { expect, request, test } from '@playwright/test';
import { readFile, rm } from 'node:fs/promises';
import path from 'node:path';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const sandbox = process.env.INTEGRATION_CONTROLLED_SANDBOX_ROOT!;
const suffix = `us46-${Date.now()}`;
const driverId = '40000000-0000-0000-0000-000000000005';
const tripId = '70000000-0000-0000-0000-000000000005';
type Auth = { accessToken: string; refreshToken: string };
type Batch = { id: string; lifecycle: string; version: number; exportEventId?: string;
  totals: { tripEarnings: string; allowances: string; overtime: string; deductions: string;
    provisionalNetInput: string }; lines: Array<{ id: string }> };
type Mapping = { id: string; externalWorkerReference: string; version: number; updatedAt: string };

test.describe.serial('US-46 real Driver payroll-input acceptance', () => {
  let admin: Auth; let approver: Auth; let limited: Auth; let tenantB: Auth;
  let batch: Batch; let original: Batch; let integrationId: string; let mappingM1: Mapping;

  test.beforeAll(async () => {
    admin = await login(); const api = await authorized(admin);
    approver = await createUser(api, 'approver', ['DRIVER_PAYROLL_VIEW', 'DRIVER_PAYROLL_APPROVE',
      'INTEGRATION_VIEW', 'INTEGRATION_ACTIVATE']);
    limited = await createUser(api, 'limited', ['DRIVER_PAYROLL_VIEW']);
    const tenant = await api.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(tenant.status(), await tenant.text()).toBe(201);
    const other = await tenant.json() as { username: string; password: string };
    tenantB = await login(other.username, other.password);
    const integration = await api.post('/api/v1/integrations', { data: integrationPayload() });
    expect(integration.status(), await integration.text()).toBe(201);
    const created = await integration.json() as { id: string; version: number }; integrationId = created.id;
    const tested = await api.post(`/api/v1/integrations/${integrationId}/test`);
    expect(tested.status(), await tested.text()).toBe(200);
    const testedConfig = (await tested.json() as { integration: { version: number } }).integration;
    const approverApi = await authorized(approver);
    const enabled = await approverApi.post(`/api/v1/integrations/${integrationId}/enable`, {
      data: { version: testedConfig.version },
    });
    expect(enabled.status(), await enabled.text()).toBe(200);
    await approverApi.dispose(); await api.dispose();
  });

  test.afterAll(async () => { await rm(sandbox, { recursive: true, force: true }); });

  test('1/6 creates a regular batch from a real completed Trip and calculates all categories', async () => {
    const api = await authorized(admin);
    const created = await api.post('/api/v1/drivers/payroll-input-batches', {
      headers: { 'Idempotency-Key': `${suffix}-batch` }, data: {
        type: 'REGULAR', periodStart: date(-30), periodEndExclusive: date(1),
        cutoffAt: new Date().toISOString(), currency: 'LKR',
      },
    });
    expect(created.status(), await created.text()).toBe(201); batch = await created.json() as Batch;
    const lines = await api.put(`/api/v1/drivers/payroll-input-batches/${batch.id}/lines`, { data: {
      version: batch.version, lines: [
        line('TRIP_EARNING', 'TRIP_RATE', '1', '100.005'),
        line('ALLOWANCE', 'MEAL', '1', '20.004'),
        line('OVERTIME', 'AUTHORIZED_HOURS', '2', '15.335', 'HOUR'),
        line('DEDUCTION', 'FIXED_AMOUNT', '1', '0', 'FIXED', '5.00'),
      ],
    } });
    expect(lines.status(), await lines.text()).toBe(200); batch = await lines.json() as Batch;
    expect(batch.totals).toMatchObject({ tripEarnings: 100.01, allowances: 20, overtime: 30.68,
      deductions: 5, provisionalNetInput: 145.69 }); await api.dispose();
  });

  test('2/6 requires a worker mapping and validates immutable source snapshots', async () => {
    const api = await authorized(admin);
    const missing = await api.post(`/api/v1/drivers/payroll-input-batches/${batch.id}/validate`, {
      data: { version: batch.version },
    });
    expect(missing.status()).toBe(400);
    expect((await missing.json() as { code: string }).code).toBe('DRIVER_PAYROLL_WORKER_MAPPING_REQUIRED');
    const mapping = await api.put(`/api/v1/drivers/${driverId}/payroll-worker-mapping`, {
      headers: { 'Idempotency-Key': `${suffix}-mapping` }, data: {
        externalSystemAlias: 'CONTROLLED_PAYROLL', externalWorkerReference: `WORKER-${suffix}`,
        active: true, version: 0,
      },
    });
    expect(mapping.status(), await mapping.text()).toBe(200); mappingM1 = await mapping.json() as Mapping;
    const replay = await api.put(`/api/v1/drivers/${driverId}/payroll-worker-mapping`, {
      headers: { 'Idempotency-Key': `${suffix}-mapping` }, data: {
        externalSystemAlias: 'CONTROLLED_PAYROLL', externalWorkerReference: `WORKER-${suffix}`,
        active: true, version: 0,
      },
    });
    expect(replay.status(), await replay.text()).toBe(200);
    expect(await replay.json()).toEqual(mappingM1);
    const conflict = await api.put(`/api/v1/drivers/${driverId}/payroll-worker-mapping`, {
      headers: { 'Idempotency-Key': `${suffix}-mapping` }, data: {
        externalSystemAlias: 'CONTROLLED_PAYROLL', externalWorkerReference: `OTHER-${suffix}`,
        active: true, version: 0,
      },
    });
    expect(conflict.status(), await conflict.text()).toBe(409);
    expect((await conflict.json() as { code: string }).code).toBe('DRIVER_PAYROLL_IDEMPOTENCY_CONFLICT');
    const validated = await api.post(`/api/v1/drivers/payroll-input-batches/${batch.id}/validate`, {
      data: { version: batch.version },
    });
    expect(validated.status(), await validated.text()).toBe(200); batch = await validated.json() as Batch;
    expect(batch.lifecycle).toBe('VALIDATED'); await api.dispose();
  });

  test('3/6 denies self-approval and allows an independent approver', async () => {
    const api = await authorized(admin);
    const denied = await api.post(`/api/v1/drivers/payroll-input-batches/${batch.id}/approve`, {
      data: { version: batch.version },
    });
    expect(denied.status()).toBe(400);
    expect((await denied.json() as { code: string }).code).toBe('DRIVER_PAYROLL_SOD_VIOLATION');
    const approverApi = await authorized(approver);
    const approved = await approverApi.post(`/api/v1/drivers/payroll-input-batches/${batch.id}/approve`, {
      data: { version: batch.version },
    });
    expect(approved.status(), await approved.text()).toBe(200); batch = await approved.json() as Batch;
    expect(batch.lifecycle).toBe('APPROVED'); await api.dispose(); await approverApi.dispose();
  });

  test('4/6 exports once through the durable integration exchange and verifies private canonical JSON', async ({ page }) => {
    const api = await authorized(admin);
    const mappingM2Response = await api.put(`/api/v1/drivers/${driverId}/payroll-worker-mapping`, {
      headers: { 'Idempotency-Key': `${suffix}-mapping-2` }, data: {
        externalSystemAlias: 'CONTROLLED_PAYROLL', externalWorkerReference: `WORKER-NEW-${suffix}`,
        active: true, version: mappingM1.version,
      },
    });
    expect(mappingM2Response.status(), await mappingM2Response.text()).toBe(200);
    const exported = await api.post(`/api/v1/drivers/payroll-input-batches/${batch.id}/export`, {
      data: { version: batch.version },
    });
    expect(exported.status(), await exported.text()).toBe(200); batch = await exported.json() as Batch;
    expect(batch.lifecycle).toBe('EXPORT_REQUESTED'); const stableEvent = batch.exportEventId;
    const replay = await api.post(`/api/v1/drivers/payroll-input-batches/${batch.id}/export`, {
      data: { version: batch.version },
    });
    expect(replay.status(), await replay.text()).toBe(200);
    expect((await replay.json() as Batch).exportEventId).toBe(stableEvent);
    await expect.poll(async () => (await exchanges(api)).find(item => item.sourceEventId === stableEvent)?.id,
      { timeout: 20_000 }).toBeTruthy();
    await api.post('/api/e2e/integrations/process');
    const exchange = (await exchanges(api)).find(item => item.sourceEventId === stableEvent)!;
    expect(exchange.status).toBe('SUCCEEDED');
    const payload = JSON.parse(await readFile(path.join(sandbox, `${exchange.id}.json`), 'utf8')) as Record<string, unknown>;
    expect(Object.keys(payload).sort()).toEqual(['batchId', 'batchType', 'currency', 'cutoffAt', 'drivers',
      'generatedAt', 'periodEndExclusive', 'periodStart', 'schemaVersion', 'totals']);
    expect(JSON.stringify(payload)).not.toMatch(/email|phone|address|medical|licen[cs]e|drug|bank|tax|pension|salary/i);
    expect(JSON.stringify(payload)).toContain(`WORKER-${suffix}`);
    expect(JSON.stringify(payload)).not.toContain(`WORKER-NEW-${suffix}`);
    await expect.poll(async () => ((await (await api.get(`/api/v1/drivers/payroll-input-batches/${batch.id}`))
      .json()) as Batch).lifecycle).toBe('EXPORTED');
    await authenticatePage(page, admin); await page.goto('/drivers/payroll-input-batches');
    await page.locator('tbody tr').first().click();
    await expect(page.getByRole('row', { name: /Safe delivery status EXPORTED/ })).toBeVisible();
    await expect(page.getByText(/Payroll\/HRMS remains authoritative.*settlement.*payment/)).toBeVisible();
    original = await (await api.get(`/api/v1/drivers/payroll-input-batches/${batch.id}`)).json() as Batch;
    await api.dispose();
  });

  test('5/6 creates an explicit compensating correction without rewriting the release', async () => {
    const api = await authorized(admin); const correction = await api.post(
      `/api/v1/drivers/payroll-input-batches/${batch.id}/corrections`, {
        headers: { 'Idempotency-Key': `${suffix}-correction` }, data: {
          version: batch.version, periodStart: date(-30), periodEndExclusive: date(1),
          cutoffAt: new Date().toISOString(), currency: 'LKR',
          lines: [{ ...line('DEDUCTION', 'FIXED_AMOUNT', '1', '0', 'FIXED', '2.50'),
            originalLineId: original.lines[0].id }],
        },
      });
    expect(correction.status(), await correction.text()).toBe(201);
    let correctionBatch = await correction.json() as Batch;
    expect(correctionBatch.lifecycle).toBe('DRAFT');
    const validated = await api.post(`/api/v1/drivers/payroll-input-batches/${correctionBatch.id}/validate`, {
      data: { version: correctionBatch.version },
    });
    expect(validated.status(), await validated.text()).toBe(200); correctionBatch = await validated.json() as Batch;
    const approverApi = await authorized(approver);
    const approved = await approverApi.post(
      `/api/v1/drivers/payroll-input-batches/${correctionBatch.id}/approve`, {
        data: { version: correctionBatch.version },
      });
    expect(approved.status(), await approved.text()).toBe(200); correctionBatch = await approved.json() as Batch;
    const exported = await api.post(`/api/v1/drivers/payroll-input-batches/${correctionBatch.id}/export`, {
      data: { version: correctionBatch.version },
    });
    expect(exported.status(), await exported.text()).toBe(200); correctionBatch = await exported.json() as Batch;
    await expect.poll(async () => (await exchanges(api)).find(
      item => item.sourceEventId === correctionBatch.exportEventId)?.id, { timeout: 20_000 }).toBeTruthy();
    await api.post('/api/e2e/integrations/process');
    await expect.poll(async () => ((await (await api.get(
      `/api/v1/drivers/payroll-input-batches/${correctionBatch.id}`)).json()) as Batch).lifecycle).toBe('EXPORTED');
    const unchanged = await (await api.get(`/api/v1/drivers/payroll-input-batches/${batch.id}`)).json() as Batch;
    expect(unchanged.lifecycle).toBe('SUPERSEDED');
    expect(unchanged.lines).toEqual(original.lines); await approverApi.dispose(); await api.dispose();
  });

  test('6/6 enforces literal API RBAC and safe cross-tenant isolation', async () => {
    const limitedApi = await authorized(limited);
    expect((await limitedApi.get('/api/v1/drivers/payroll-input-batches')).status()).toBe(200);
    expect((await limitedApi.post('/api/v1/drivers/payroll-input-batches', {
      headers: { 'Idempotency-Key': `${suffix}-denied` }, data: {
        type: 'REGULAR', periodStart: date(-1), periodEndExclusive: date(1),
        cutoffAt: new Date().toISOString(), currency: 'LKR',
      },
    })).status()).toBe(403);
    const otherApi = await authorized(tenantB);
    const denied = await otherApi.get(`/api/v1/drivers/payroll-input-batches/${batch.id}`);
    expect([400, 404]).toContain(denied.status());
    expect((await denied.json() as { code: string }).code).toBe('DRIVER_PAYROLL_BATCH_NOT_FOUND');
    expect([400, 404]).toContain(
      (await otherApi.get(`/api/v1/drivers/payroll-input-batches/${batch.id}/history`)).status());
    expect([400, 404]).toContain((await otherApi.post(
      `/api/v1/drivers/payroll-input-batches/${batch.id}/approve`, { data: { version: batch.version } })).status());
    expect([400, 404]).toContain((await otherApi.post(
      `/api/v1/drivers/payroll-input-batches/${batch.id}/export`, { data: { version: batch.version } })).status());
    await limitedApi.dispose(); await otherApi.dispose();
  });

  function authorized(auth: Auth) { return request.newContext({ baseURL: backend,
    extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } }); }
  async function exchanges(api: Awaited<ReturnType<typeof request.newContext>>) {
    const response = await api.get(`/api/v1/integrations/${integrationId}/exchanges?size=100`);
    expect(response.status(), await response.text()).toBe(200);
    return (await response.json() as { content: Array<{ id: string; sourceEventId: string; status: string }> }).content;
  }
});

async function login(username = process.env.E2E_ADMIN_USERNAME ?? 'admin',
                     password = process.env.E2E_ADMIN_PASSWORD ?? 'AdminPass!2026') {
  const api = await request.newContext({ baseURL: backend });
  const response = await api.post('/api/auth/login', { data: { username, password } });
  expect(response.status(), await response.text()).toBe(200);
  const auth = await response.json() as Auth; await api.dispose(); return auth;
}
async function createUser(api: Awaited<ReturnType<typeof request.newContext>>, label: string, permissions: string[]) {
  const role = await api.post('/api/roles', { data: { name: `US46 ${label} ${suffix}`, active: true, permissions } });
  expect(role.status(), await role.text()).toBe(201); const roleId = (await role.json() as { id: string }).id;
  const username = `us46-${label}-${suffix}`; const password = `Us46!${label}-${suffix}`;
  const user = await api.post('/api/users', { data: { username, email: `${username}@example.test`, password,
    firstName: 'US46', lastName: label, active: true, roleIds: [roleId] } });
  expect(user.status(), await user.text()).toBe(201); return login(username, password);
}
function line(category: string, reasonCode: string, quantity: string, rate: string, unit = 'TRIP', amount?: string) {
  return { driverId, tripId, tripNumber: 'TRIP-2026-0005', category, reasonCode,
    description: 'Authorized source-backed operational input', quantity, unit, rate, amount };
}
function date(offset: number) { const value = new Date(); value.setUTCDate(value.getUTCDate() + offset);
  return value.toISOString().slice(0, 10); }
function integrationPayload() { return { name: `US46 controlled payroll ${suffix}`, type: 'FILE_EXCHANGE',
  protocol: 'FILE_JSON_V1', direction: 'OUTBOUND', endpointAlias: 'CONTROLLED_SANDBOX',
  dataClassification: 'FINANCIAL_CONFIDENTIAL', mapping: { mappingKey: 'DRIVER_PAYROLL_INPUT_V1',
    sourceContract: 'DRIVER_PAYROLL_INPUT_V1', sourceVersion: 1, targetSchema: 'DRIVER_PAYROLL_INPUT_V1',
    targetVersion: 1, rules: [{ sourceField: 'batchId', targetField: 'batchId', format: 'UUID',
      omitIfNull: false, required: true }] } }; }
async function authenticatePage(page: import('@playwright/test').Page, auth: Auth) {
  await page.addInitScript(value => { localStorage.setItem('transport.accessToken', value.accessToken);
    localStorage.setItem('transport.refreshToken', value.refreshToken); }, auth);
}
