import { expect, request, test } from '@playwright/test';
import { mkdir, readFile, rename, rm, unlink, writeFile } from 'node:fs/promises';
import path from 'node:path';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const sandbox = process.env.INTEGRATION_CONTROLLED_SANDBOX_ROOT!;
const suffix = `us73-${Date.now()}`;
type Auth = { accessToken: string; refreshToken: string };
type Integration = { id: string; name: string; lifecycle: string; health: string; version: number };
type Exchange = { id: string; sourceEventId: string; status: string; attemptCount: number; targetFilename?: string; lastErrorCode?: string };
type Page<T> = { content: T[]; totalElements: number };

async function login(username = process.env.E2E_ADMIN_USERNAME ?? 'admin',
                     password = process.env.E2E_ADMIN_PASSWORD ?? 'AdminPass!2026') {
  const api = await request.newContext({ baseURL: backend });
  const response = await api.post('/api/auth/login', { data: { username, password } });
  expect(response.status(), await response.text()).toBe(200);
  const auth = await response.json() as Auth; await api.dispose(); return auth;
}

const mapping = { mappingKey: 'US73_PLATFORM_PROBE', sourceContract: 'US73_PLATFORM_PROBE', sourceVersion: 1,
  targetSchema: 'US73_FILE_PROBE', targetVersion: 1, rules: [
    { sourceField: 'probeId', targetField: 'probe_id', format: 'UUID', omitIfNull: false, required: true },
    { sourceField: 'probeType', targetField: 'probe_type', format: 'ENUM', omitIfNull: false, required: true },
    { sourceField: 'sequence', targetField: 'sequence', format: 'DECIMAL', omitIfNull: false, required: true },
  ] };

test.describe.serial('US-73 real PostgreSQL and filesystem integration acceptance', () => {
  let admin: Auth; let limited: Auth; let tenantB: Auth; let integration: Integration; let initialExchange: Exchange;

  test.beforeAll(async () => {
    await mkdir(sandbox, { recursive: true }); admin = await login();
    const api = await authorized(admin);
    const role = await api.post('/api/roles', { data: { name: `US73 limited ${suffix}`, active: true,
      permissions: ['INTEGRATION_VIEW'] } });
    expect(role.status(), await role.text()).toBe(201);
    const roleId = (await role.json() as { id: string }).id;
    const username = `us73-limited-${suffix}`; const password = `Us73Limited!${suffix}`;
    const user = await api.post('/api/users', { data: { username, email: `${username}@example.test`, password,
      firstName: 'US73', lastName: 'Limited', active: true, roleIds: [roleId] } });
    expect(user.status(), await user.text()).toBe(201); limited = await login(username, password);
    const tenant = await api.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(tenant.status(), await tenant.text()).toBe(201);
    const tenantFixture = await tenant.json() as { username: string; password: string };
    tenantB = await login(tenantFixture.username, tenantFixture.password); await api.dispose();
  });

  test.afterAll(async () => { await rm(sandbox, { recursive: true, force: true }); });

  test('1/6 rejects an invalid mapping and creates only a governed DRAFT configuration', async ({ page }) => {
    const api = await authorized(admin); const invalid = payload();
    invalid.mapping.rules[2].targetField = 'probe_type';
    const rejected = await api.post('/api/v1/integrations', { data: invalid });
    expect(rejected.status(), await rejected.text()).toBe(400);
    expect((await rejected.json() as { code: string }).code).toBe('INTEGRATION_MAPPING_INVALID');
    const created = await api.post('/api/v1/integrations', { data: payload() });
    expect(created.status(), await created.text()).toBe(201); integration = await created.json() as Integration;
    expect(integration.lifecycle).toBe('DRAFT'); expect(integration.health).toBe('UNKNOWN');
    expect(integration).not.toHaveProperty('credentialReference');
    expect(integration).not.toHaveProperty('tenantId');
    await authenticatePage(page, admin); await page.goto('/integrations');
    await expect(page.getByRole('link', { name: integration.name })).toBeVisible(); await api.dispose();
  });

  test('2/6 tests, enables, crosses the shared outbox, writes JSON, and renders safe history', async ({ page }) => {
    const api = await authorized(admin);
    const tested = await api.post(`/api/v1/integrations/${integration.id}/test`);
    expect(tested.status(), await tested.text()).toBe(200);
    const testResult = await tested.json() as { success: boolean; integration: Integration };
    expect(testResult.success).toBe(true); expect(testResult.integration.health).toBe('HEALTHY');
    const enabled = await api.post(`/api/v1/integrations/${integration.id}/enable`, {
      data: { version: testResult.integration.version },
    });
    expect(enabled.status(), await enabled.text()).toBe(200); integration = await enabled.json() as Integration;
    await expect.poll(async () => (await history(api)).content.length, { timeout: 20_000 }).toBeGreaterThan(0);
    await process(api); await expect.poll(async () => (await history(api)).content[0]?.status).toBe('SUCCEEDED');
    initialExchange = (await history(api)).content[0];
    const output = JSON.parse(await readFile(path.join(sandbox, `${initialExchange.id}.json`), 'utf8')) as Record<string, unknown>;
    expect(Object.keys(output).sort()).toEqual(['probe_id', 'probe_type', 'sequence']);
    await authenticatePage(page, admin); await page.goto(`/integrations/${integration.id}`);
    await expect(page.getByText('Exchange history')).toBeVisible();
    await expect(page.getByText(`${initialExchange.id}.json`)).toBeVisible();
    await expect(page.getByText(sandbox)).toHaveCount(0); await api.dispose();
  });

  test('3/6 deduplicates a durable logical replay and produces one exchange and one file', async () => {
    const api = await authorized(admin); const eventId = crypto.randomUUID(); const probeId = crypto.randomUUID();
    for (let index = 0; index < 2; index++) {
      const replay = await api.post(`/api/e2e/integrations/${integration.id}/replay`, {
        data: { eventId, probeId, sequence: 73 },
      });
      expect(replay.status(), await replay.text()).toBe(200);
    }
    await expect.poll(async () => (await history(api)).content.filter(item => item.sourceEventId === eventId).length,
      { timeout: 20_000 }).toBe(1);
    await process(api); const matches = (await history(api)).content.filter(item => item.sourceEventId === eventId);
    expect(matches).toHaveLength(1); expect(matches[0].status).toBe('SUCCEEDED');
    expect(await readFile(path.join(sandbox, `${matches[0].id}.json`), 'utf8')).toContain(probeId);
    await process(api); expect((await history(api)).content.filter(item => item.sourceEventId === eventId)).toHaveLength(1);
    await api.dispose();
  });

  test('4/6 classifies temporary filesystem failure, schedules retry, and later succeeds', async () => {
    test.setTimeout(80_000); const api = await authorized(admin); const eventId = crypto.randomUUID();
    await replay(api, eventId); const pending = await waitForExchange(api, eventId);
    const backup = `${sandbox}-temporary-backup`; await rename(sandbox, backup); await writeFile(sandbox, 'blocked');
    await process(api); await unlink(sandbox); await rename(backup, sandbox);
    await expect.poll(async () => (await byEvent(api, eventId)).status).toBe('RETRY_SCHEDULED');
    expect((await byEvent(api, eventId)).attemptCount).toBe(1);
    await new Promise(resolve => setTimeout(resolve, 31_000)); await process(api);
    await expect.poll(async () => (await byEvent(api, eventId)).status).toBe('SUCCEEDED');
    expect(await readFile(path.join(sandbox, `${pending.id}.json`), 'utf8')).toContain('CONTROLLED_SANDBOX');
    await api.dispose();
  });

  test('5/6 makes integrity mismatch terminal and a disabled configuration accepts no new exchange', async () => {
    const api = await authorized(admin); const eventId = crypto.randomUUID(); await replay(api, eventId);
    const pending = await waitForExchange(api, eventId);
    await writeFile(path.join(sandbox, `${pending.id}.json`), '{"tampered":true}'); await process(api);
    const failed = await byEvent(api, eventId); expect(failed.status).toBe('FAILED');
    expect(failed.lastErrorCode).toBe('INTEGRATION_FILE_INTEGRITY_FAILURE');
    const current = await (await api.get(`/api/v1/integrations/${integration.id}`)).json() as Integration;
    const disabled = await api.post(`/api/v1/integrations/${integration.id}/disable`, { data: { version: current.version } });
    expect(disabled.status(), await disabled.text()).toBe(200); integration = await disabled.json() as Integration;
    const before = (await history(api)).totalElements; await replay(api, crypto.randomUUID());
    await new Promise(resolve => setTimeout(resolve, 7_000)); expect((await history(api)).totalElements).toBe(before);
    await api.dispose();
  });

  test('6/6 enforces mutation permission and safe cross-Tenant not-found semantics', async () => {
    const limitedApi = await authorized(limited);
    expect((await limitedApi.get('/api/v1/integrations')).status()).toBe(200);
    expect((await limitedApi.post('/api/v1/integrations', { data: payload() })).status()).toBe(403);
    expect((await limitedApi.post(`/api/v1/integrations/${integration.id}/test`)).status()).toBe(403);
    const otherApi = await authorized(tenantB);
    expect((await otherApi.get(`/api/v1/integrations/${integration.id}`)).status()).toBe(404);
    expect((await otherApi.get('/api/v1/integrations')).status()).toBe(200);
    expect(((await (await otherApi.get('/api/v1/integrations')).json()) as Page<Integration>).content
      .some(item => item.id === integration.id)).toBe(false);
    await limitedApi.dispose(); await otherApi.dispose();
  });

  function authorized(auth: Auth) { return request.newContext({ baseURL: backend,
    extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } }); }
  function payload() { return { name: `Controlled sandbox ${suffix}`, type: 'FILE_EXCHANGE', protocol: 'FILE_JSON_V1',
    direction: 'OUTBOUND', endpointAlias: 'CONTROLLED_SANDBOX', dataClassification: 'INTERNAL_OPERATIONAL_NON_SENSITIVE',
    mapping: structuredClone(mapping) }; }
  async function history(api: Awaited<ReturnType<typeof request.newContext>>) {
    const response = await api.get(`/api/v1/integrations/${integration.id}/exchanges?size=100`);
    expect(response.status(), await response.text()).toBe(200); return await response.json() as Page<Exchange>;
  }
  async function process(api: Awaited<ReturnType<typeof request.newContext>>) {
    const response = await api.post('/api/e2e/integrations/process'); expect(response.status(), await response.text()).toBe(200);
  }
  async function replay(api: Awaited<ReturnType<typeof request.newContext>>, eventId: string) {
    const response = await api.post(`/api/e2e/integrations/${integration.id}/replay`, {
      data: { eventId, probeId: crypto.randomUUID(), sequence: Date.now() },
    }); expect(response.status(), await response.text()).toBe(200);
  }
  async function byEvent(api: Awaited<ReturnType<typeof request.newContext>>, eventId: string) {
    const match = (await history(api)).content.find(item => item.sourceEventId === eventId);
    expect(match).toBeDefined(); return match!;
  }
  async function waitForExchange(api: Awaited<ReturnType<typeof request.newContext>>, eventId: string) {
    await expect.poll(async () => (await history(api)).content.some(item => item.sourceEventId === eventId),
      { timeout: 20_000 }).toBe(true); return byEvent(api, eventId);
  }
  async function authenticatePage(page: import('@playwright/test').Page, auth: Auth) {
    await page.addInitScript(value => { localStorage.setItem('transport.accessToken', value.accessToken);
      localStorage.setItem('transport.refreshToken', value.refreshToken); }, auth);
  }
});
