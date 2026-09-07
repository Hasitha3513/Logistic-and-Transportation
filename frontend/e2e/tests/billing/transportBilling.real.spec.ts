import { expect, request, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { readFile, rm } from 'node:fs/promises';
import path from 'node:path';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const sandbox = process.env.INTEGRATION_CONTROLLED_SANDBOX_ROOT!;
const suffix = `us47-${Date.now()}`;
const closedTripId = '70000000-0000-0000-0000-000000000010';
const completedTripId = '70000000-0000-0000-0000-000000000005';
const freightId = '90000000-0000-0000-0000-000000000010';
const idempotentFreightId = '90000000-0000-0000-0000-000000000008';
type Auth = { accessToken: string; refreshToken: string };
type Billing = { id: string; billingNumber: string; recordType: string; originalBillingRecordId?: string;
  source: { type: string; id: string; businessNumber: string; snapshotHash: string };
  customerId: string; currency: string; lifecycle: string; version: number; exportEventId?: string;
  lines: Array<{ category: string; amount: number }>; tax: { status: string; taxAmount: number };
  costCentres: Array<{ code: string; allocationPercent: number }>;
  totals: { baseCharge: number; surcharges: number; penalties: number; creditAdjustments: number;
    subtotal: number; taxAmount: number; totalAmount: number } };

test.describe.serial('US-47 real transport billing acceptance', () => {
  let admin: Auth; let approver: Auth; let limited: Auth; let tenantB: Auth;
  let billing: Billing; let original: Billing; let integrationId: string;
  let canonicalFile: Buffer; let recordedPayloadHash: string;

  test.beforeAll(async () => {
    seedOwnerFacts();
    admin = await login(); const api = await authorized(admin);
    approver = await createUser(api, 'approver', ['BILLING_VIEW', 'BILLING_APPROVE',
      'INTEGRATION_VIEW', 'INTEGRATION_ACTIVATE']);
    limited = await createUser(api, 'limited', ['BILLING_VIEW']);
    const tenant = await api.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(tenant.status(), await tenant.text()).toBe(201);
    const other = await tenant.json() as { username: string; password: string };
    tenantB = await login(other.username, other.password);
    const integration = await api.post('/api/v1/integrations', { data: integrationPayload() });
    expect(integration.status(), await integration.text()).toBe(201);
    const created = await integration.json() as { id: string }; integrationId = created.id;
    const tested = await api.post(`/api/v1/integrations/${integrationId}/test`);
    expect(tested.status(), await tested.text()).toBe(200);
    const version = (await tested.json() as { integration: { version: number } }).integration.version;
    const approverApi = await authorized(approver);
    const enabled = await approverApi.post(`/api/v1/integrations/${integrationId}/enable`, { data: { version } });
    expect(enabled.status(), await enabled.text()).toBe(200);
    await approverApi.dispose(); await api.dispose();
  });

  test.afterAll(async () => { await rm(sandbox, { recursive: true, force: true }); });

  test('1/7 bills a CLOSED Trip with all categories, supplied tax, cost centre, SoD and export', async () => {
    const api = await authorized(admin);
    const created = await api.post('/api/v1/billing/records', {
      headers: key('trip-create'), data: { sourceType: 'TRIP', sourceId: closedTripId, currency: 'LKR' },
    });
    expect(created.status(), await created.text()).toBe(201); billing = await created.json() as Billing;
    const replaced = await api.put(`/api/v1/billing/records/${billing.id}/lines`, { data: replacement(billing.version) });
    expect(replaced.status(), await replaced.text()).toBe(200); billing = await replaced.json() as Billing;
    expect(billing.totals).toMatchObject({ baseCharge: 100, surcharges: 10, penalties: 5,
      creditAdjustments: 3, subtotal: 112, taxAmount: 11.2, totalAmount: 123.2 });
    const validated = await api.post(`/api/v1/billing/records/${billing.id}/validate`, { data: { version: billing.version } });
    expect(validated.status(), await validated.text()).toBe(200); billing = await validated.json() as Billing;
    const self = await api.post(`/api/v1/billing/records/${billing.id}/approve`, {
      headers: key('self-approve'), data: { version: billing.version },
    });
    expect(self.status()).toBe(400); expect((await self.json() as { code: string }).code).toBe('BILLING_SOD_VIOLATION');
    const approverApi = await authorized(approver);
    const approved = await approverApi.post(`/api/v1/billing/records/${billing.id}/approve`, {
      headers: key('approve'), data: { version: billing.version },
    });
    expect(approved.status(), await approved.text()).toBe(200); billing = await approved.json() as Billing;
    const finalized = await api.post(`/api/v1/billing/records/${billing.id}/finalize`, {
      headers: key('finalize'), data: { version: billing.version },
    });
    expect(finalized.status(), await finalized.text()).toBe(200); billing = await finalized.json() as Billing;
    const exported = await api.post(`/api/v1/billing/records/${billing.id}/export`, {
      headers: key('export'), data: { version: billing.version },
    });
    expect(exported.status(), await exported.text()).toBe(200); billing = await exported.json() as Billing;
    expect(billing.lifecycle).toBe('EXPORT_REQUESTED'); const event = billing.exportEventId!;
    await expect.poll(async () => (await exchanges(api)).find(x => x.sourceEventId === event)?.id,
      { timeout: 60_000 }).toBeTruthy();
    const processed = await api.post('/api/e2e/integrations/process');
    expect(processed.status(), await processed.text()).toBe(200);
    await expect.poll(async () => (await exchanges(api)).find(x => x.sourceEventId === event)?.status,
      { timeout: 20_000 }).toBe('SUCCEEDED');
    const exchange = (await exchanges(api)).find(x => x.sourceEventId === event)!;
    canonicalFile = await readFile(path.join(sandbox, `${exchange.id}.json`));
    recordedPayloadHash = exchange.payloadHash;
    await expect.poll(async () => ((await (await api.get(`/api/v1/billing/records/${billing.id}`)).json()) as Billing).lifecycle)
      .toBe('EXPORTED');
    billing = await (await api.get(`/api/v1/billing/records/${billing.id}`)).json() as Billing;
    await approverApi.dispose(); await api.dispose();
  });

  test('2/7 rejects non-terminal sources and accepts an explicit Freight owner fact', async () => {
    const api = await authorized(admin);
    const trip = await api.post('/api/v1/billing/records', { headers: key('completed-trip'),
      data: { sourceType: 'TRIP', sourceId: completedTripId, currency: 'LKR' } });
    expect(trip.status()).toBe(400); expect((await trip.json() as { code: string }).code).toBe('BILLING_SOURCE_NOT_ELIGIBLE');
    const absentFreight = await api.post('/api/v1/billing/records', { headers: key('absent-freight'),
      data: { sourceType: 'FREIGHT_ORDER', sourceId: '90000000-0000-0000-0000-000000000009', currency: 'LKR' } });
    expect(absentFreight.status()).toBe(400);
    const freight = await api.post('/api/v1/billing/records', { headers: key('freight'),
      data: { sourceType: 'FREIGHT_ORDER', sourceId: freightId, currency: 'LKR' } });
    expect(freight.status(), await freight.text()).toBe(201);
    expect((await freight.json() as Billing).source).toMatchObject({ type: 'FREIGHT_ORDER', id: freightId });
    await api.dispose();
  });

  test('3/7 rejects invalid currency, stale edit and duplicate source safely', async () => {
    const api = await authorized(admin);
    const currency = await api.post('/api/v1/billing/records', { headers: key('currency'),
      data: { sourceType: 'TRIP', sourceId: closedTripId, currency: 'USD' } });
    expect(currency.status()).toBe(400);
    const duplicate = await api.post('/api/v1/billing/records', { headers: key('duplicate'),
      data: { sourceType: 'TRIP', sourceId: closedTripId, currency: 'LKR' } });
    expect(duplicate.status()).toBe(400);
    const stale = await api.put(`/api/v1/billing/records/${billing.id}/lines`, { data: replacement(0) });
    expect(stale.status()).toBe(409); await api.dispose();
  });

  test('4/7 keeps finalized facts immutable and finalizes one exact reversal', async () => {
    const api = await authorized(admin);
    original = await (await api.get(`/api/v1/billing/records/${billing.id}`)).json() as Billing;
    const edit = await api.put(`/api/v1/billing/records/${billing.id}/lines`, { data: replacement(original.version) });
    expect(edit.status()).toBe(400);
    const reversed = await api.post(`/api/v1/billing/records/${billing.id}/reversals`, {
      headers: key('reverse'), data: { version: original.version, reason: 'Exact commercial reversal' },
    });
    expect(reversed.status(), await reversed.text()).toBe(201); let reversal = await reversed.json() as Billing;
    expect(reversal.recordType).toBe('REVERSAL'); expect(reversal.originalBillingRecordId).toBe(billing.id);
    expect(reversal.totals.totalAmount).toBe(-original.totals.totalAmount);
    reversal = await command(api, reversal, 'validate');
    const approverApi = await authorized(approver); reversal = await command(approverApi, reversal, 'approve', true);
    reversal = await command(api, reversal, 'finalize', true);
    expect(reversal.lifecycle).toBe('FINALIZED');
    const unchanged = await (await api.get(`/api/v1/billing/records/${billing.id}`)).json() as Billing;
    expect(unchanged.lifecycle).toBe('REVERSED'); expect(unchanged.lines).toEqual(original.lines);
    const twice = await api.post(`/api/v1/billing/records/${billing.id}/reversals`, {
      headers: key('reverse-twice'), data: { version: unchanged.version, reason: 'Duplicate reversal' },
    });
    expect(twice.status()).toBe(400); await approverApi.dispose(); await api.dispose();
  });

  test('5/7 replays identical idempotent requests and conflicts on changed material', async () => {
    const api = await authorized(admin); const createKey = key('idempotent-create');
    const body = { sourceType: 'FREIGHT_ORDER', sourceId: idempotentFreightId, currency: 'LKR' };
    const first = await api.post('/api/v1/billing/records', { headers: createKey, data: body });
    const replay = await api.post('/api/v1/billing/records', { headers: createKey, data: body });
    expect(first.status()).toBe(201); expect(replay.status()).toBe(201);
    expect((await replay.json() as Billing).id).toBe((await first.json() as Billing).id);
    const conflict = await api.post('/api/v1/billing/records', { headers: createKey,
      data: { ...body, currency: 'USD' } });
    expect(conflict.status()).toBe(409); await api.dispose();
  });

  test('6/7 enforces literal API permissions and Tenant-B isolation', async () => {
    const limitedApi = await authorized(limited);
    expect((await limitedApi.get('/api/v1/billing/records')).status()).toBe(200);
    expect((await limitedApi.post('/api/v1/billing/records', { headers: key('limited'),
      data: { sourceType: 'TRIP', sourceId: closedTripId, currency: 'LKR' } })).status()).toBe(403);
    const otherApi = await authorized(tenantB);
    const hidden = await otherApi.get(`/api/v1/billing/records/${billing.id}`);
    expect([400, 404]).toContain(hidden.status());
    expect((await hidden.json() as { code: string }).code).toBe('BILLING_NOT_FOUND');
    await limitedApi.dispose(); await otherApi.dispose();
  });

  test('7/7 verifies controlled canonical private JSON, hash, EXPORTED truth and UI', async ({ page }) => {
    const api = await authorized(admin);
    expect(createHash('sha256').update(canonicalFile).digest('hex')).toBe(recordedPayloadHash);
    const canonical = canonicalFile.toString('utf8'); const payload = JSON.parse(canonical) as Record<string, unknown>;
    expect(JSON.stringify(payload)).toBe(canonical);
    expect(Object.keys(payload)).toEqual(['amounts', 'billingNumber', 'billingRecordId', 'costCentres', 'currency',
      'customerId', 'finalizedAt', 'originalBillingRecordId', 'recordType', 'schemaVersion', 'source', 'tax']);
    expect(canonical).not.toMatch(/customerName|email|phone|address|cargo|note|credential|bank|accountNumber/i);
    expect(payload.amounts).toMatchObject({ baseCharge: '100.00', surcharges: '10.00', penalties: '5.00',
      creditAdjustments: '3.00', subtotal: '112.00', taxAmount: '11.20', totalAmount: '123.20' });
    expect(payload.costCentres).toEqual([{ code: 'OPS', allocationPercent: '100.0000' }]);
    await authenticatePage(page, admin); await page.goto('/billing/records');
    await page.getByText(billing.billingNumber).click();
    await expect(page.getByText('Operational transport billing')).toBeVisible();
    await expect(page.getByText(/does not claim tax invoicing, accounting posting, settlement, or payment/)).toBeVisible();
    await expect(page.getByText(/BASE_CHARGE/)).toBeVisible(); await expect(page.getByText(/PENALTY/)).toBeVisible();
    await api.dispose();
  });

  function authorized(auth: Auth) { return request.newContext({ baseURL: backend,
    extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } }); }
  async function exchanges(api: Awaited<ReturnType<typeof request.newContext>>) {
    const response = await api.get(`/api/v1/integrations/${integrationId}/exchanges?size=100`);
    expect(response.status(), await response.text()).toBe(200);
    return (await response.json() as { content: Array<{ id: string; sourceEventId: string; status: string;
      payloadHash: string }> }).content;
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
  const role = await api.post('/api/roles', { data: { name: `US47 ${label} ${suffix}`, active: true, permissions } });
  expect(role.status(), await role.text()).toBe(201); const roleId = (await role.json() as { id: string }).id;
  const username = `us47-${label}-${suffix}`; const password = `Us47!${label}-${suffix}`;
  const user = await api.post('/api/users', { data: { username, email: `${username}@example.test`, password,
    firstName: 'US47', lastName: label, active: true, roleIds: [roleId] } });
  expect(user.status(), await user.text()).toBe(201); return login(username, password);
}
function key(label: string) { return { 'Idempotency-Key': `${suffix}-${label}` }; }
function replacement(version: number) { return { version, lines: [
  { category: 'BASE_CHARGE', reasonCode: 'CONTRACT', provenance: 'Approved transport contract', quantity: '1', unitRate: '100', amount: '100' },
  { category: 'SURCHARGE', reasonCode: 'HANDLING', provenance: 'Approved handling fact', quantity: '1', unitRate: '10', amount: '10' },
  { category: 'PENALTY', reasonCode: 'WAITING', provenance: 'Approved customer debit fact', quantity: '1', unitRate: '5', amount: '5' },
  { category: 'CREDIT_ADJUSTMENT', reasonCode: 'SERVICE_CREDIT', provenance: 'Approved bounded correction', quantity: '1', unitRate: '3', amount: '3' },
], tax: { status: 'SUPPLIED', category: 'VAT', jurisdictionReference: 'LK', taxableAmount: '112',
  rate: '10.0000', taxAmount: '11.20', provenance: 'Externally supplied tax fact', snapshotHash: 'a'.repeat(64) },
costCentres: [{ code: 'OPS', allocationPercent: '100.0000', description: 'Operations', source: 'Approved allocation' }] }; }
async function command(api: Awaited<ReturnType<typeof request.newContext>>, item: Billing,
                       action: 'validate'|'approve'|'finalize', idempotent = false) {
  const response = await api.post(`/api/v1/billing/records/${item.id}/${action}`, {
    headers: idempotent ? key(`${action}-${item.id}`) : undefined, data: { version: item.version },
  });
  expect(response.status(), await response.text()).toBe(200); return await response.json() as Billing;
}
function integrationPayload() { return { name: `US47 controlled billing ${suffix}`, type: 'FILE_EXCHANGE',
  protocol: 'FILE_JSON_V1', direction: 'OUTBOUND', endpointAlias: 'CONTROLLED_SANDBOX',
  dataClassification: 'FINANCIAL_CONFIDENTIAL', mapping: { mappingKey: 'TRANSPORT_BILLING_V1',
    sourceContract: 'TRANSPORT_BILLING_V1', sourceVersion: 1, targetSchema: 'TRANSPORT_BILLING_V1',
    targetVersion: 1, rules: [{ sourceField: 'billingRecordId', targetField: 'billingRecordId', format: 'UUID',
      omitIfNull: false, required: true }] } }; }
async function authenticatePage(page: import('@playwright/test').Page, auth: Auth) {
  await page.addInitScript(value => { localStorage.setItem('transport.accessToken', value.accessToken);
    localStorage.setItem('transport.refreshToken', value.refreshToken); }, auth);
}
function seedOwnerFacts() {
  const database = process.env.PGDATABASE; const user = process.env.PGUSER;
  if (database !== 'transport_logistics_acceptance' || !user) throw new Error('US-47 fixtures require transport_logistics_acceptance');
  const sql = `TRUNCATE transport_billing_record CASCADE; UPDATE integration_configuration c SET lifecycle='DISABLED',version=version+1,updated_at=CURRENT_TIMESTAMP FROM integration_mapping m WHERE c.current_mapping_id=m.id AND m.source_contract='TRANSPORT_BILLING_V1' AND c.lifecycle='ACTIVE'; UPDATE trip SET status='CLOSED',updated_at=CURRENT_TIMESTAMP WHERE id='${closedTripId}' AND tenant_id='4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a'; INSERT INTO freight_billing_fact(freight_order_id,tenant_id,lifecycle,completed_at,source_version) VALUES('${freightId}','4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a','COMPLETED',CURRENT_TIMESTAMP,1),('${idempotentFreightId}','4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a','CLOSED',CURRENT_TIMESTAMP,1) ON CONFLICT(freight_order_id) DO UPDATE SET lifecycle=EXCLUDED.lifecycle,completed_at=EXCLUDED.completed_at,source_version=1`;
  execFileSync('docker', ['compose', 'exec', '-T', 'postgres', 'psql', '-X', '--no-psqlrc', '-U', user,
    '-d', database, '-v', 'ON_ERROR_STOP=1', '-c', sql], { encoding: 'utf8', env: process.env });
}
