import { expect, request, test } from '@playwright/test';
import { seedGpsExceptionEvidence } from '../../utils/gpsExceptionAcceptanceFixture';
const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088', suffix = `us55-${Date.now()}`;
type Auth = { accessToken: string; refreshToken: string };
let viewer: Auth, reviewer: Auth, reviewOnly: Auth, tenantB: Auth;
let fixture: ReturnType<typeof seedGpsExceptionEvidence>;

test.describe.serial('US-55 GPS exceptions CS07 real Chromium journey', () => {
  test.beforeAll(async () => {
    const root = await authorized(await login());
    const tenant = await root.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(tenant.status(), await tenant.text()).toBe(201);
    const created = await tenant.json() as { tenantId: string; username: string; password: string };
    const tenantAdmin = await login(created.username, created.password);
    const api = await authorized(tenantAdmin);
    viewer = await createUser(api, ['GPS_EXCEPTION_VIEW'], 'viewer');
    reviewer = await createUser(api, ['GPS_EXCEPTION_VIEW', 'GPS_EXCEPTION_REVIEW'], 'reviewer');
    reviewOnly = await createUser(api, ['GPS_EXCEPTION_REVIEW'], 'review-only');
    fixture = seedGpsExceptionEvidence(created.tenantId);
    const other = await root.post('/api/e2e/tenant-fixtures', { data: { suffix: `${suffix}-b` } });
    expect(other.status(), await other.text()).toBe(201);
    const foreign = await other.json() as { username: string; password: string };
    tenantB = await login(foreign.username, foreign.password);
    await api.dispose(); await root.dispose();
  });

  test('1/6 provides permission-aware navigation and explicit range validation', async ({ page }) => {
    await authenticate(page, viewer); await page.goto('/tracking/gps-exceptions');
    await expect(page.getByRole('menuitem', { name: 'GPS Exceptions' })).toBeVisible();
    await expect(page.getByText('A valid UTC range is required')).toBeVisible();
    await load(page); await expect(page.getByRole('row', { name: new RegExp(fixture.vehicleId) })).toBeVisible();
    expect(page.url()).not.toContain(fixture.vehicleId);
  });

  test('2/6 shows minimized immutable detail and evidence to VIEW-only users', async ({ page }) => {
    await authenticate(page, viewer); await load(page); await page.getByRole('button', { name: `View GPS exception ${fixture.episodeId}` }).click();
    await expect(page.getByRole('heading', { name: 'Immutable evidence' })).toBeVisible();
    await expect(page.getByText('Clock skew')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Acknowledge review' })).toHaveCount(0);
    await expect(page.locator('body')).not.toContainText('us55-controlled-provider-secret');
    await page.screenshot({ path: '../docs/evidence/us55-cs07-gps-exception-detail.png', fullPage: true });
  });

  test('3/6 denies REVIEW-only deep links without issuing evidence requests', async ({ page }) => {
    await authenticate(page, reviewOnly); await page.goto('/tracking/gps-exceptions');
    await expect(page.getByText('GPS exception access denied')).toBeVisible();
    await expect(page.getByRole('table', { name: 'GPS exception episodes' })).toHaveCount(0);
  });

  test('4/6 returns safe foreign-Tenant absence', async () => {
    const api = await authorized(tenantB);
    const response = await api.get(`/api/v1/tracking/gps-exceptions/${fixture.episodeId}`);
    expect(response.status()).toBe(404); expect(await response.text()).not.toContain(fixture.vehicleId);
    await api.dispose();
  });

  test('5/6 authorized review records one acknowledgement with keyboard-operable modal', async ({ page }) => {
    await authenticate(page, reviewer); await load(page); await page.getByRole('button', { name: `View GPS exception ${fixture.episodeId}` }).click();
    await page.getByRole('button', { name: 'Acknowledge review' }).focus(); await page.keyboard.press('Enter');
    const dialog = page.getByRole('dialog', { name: 'Acknowledge GPS exception' }); await expect(dialog).toBeVisible();
    await expect(dialog.getByLabel('Acknowledgement reason')).toBeFocused();
    await dialog.getByLabel('Acknowledgement reason').fill('Controlled operator review');
    await dialog.getByRole('button', { name: 'Acknowledge' }).click();
    await expect(dialog).toBeHidden(); await expect(page.getByRole('dialog', { name: 'GPS exception detail' }).getByText('Acknowledged', { exact: true })).toBeVisible();
  });

  test('6/6 acknowledged episodes remain read-only and detector-controlled', async ({ page }) => {
    await authenticate(page, reviewer); await load(page); await page.getByRole('button', { name: `View GPS exception ${fixture.episodeId}` }).click();
    await expect(page.getByText('Acknowledgement unavailable while Acknowledged')).toBeVisible();
    await expect(page.getByRole('button', { name: /resolve|recover|severity/i })).toHaveCount(0);
  });
});

async function load(page: import('@playwright/test').Page) {
  if (!page.url().includes('/tracking/gps-exceptions')) await page.goto('/tracking/gps-exceptions');
  await page.getByLabel('GPS exception range from').fill(local(fixture.from)); await page.getByLabel('GPS exception range to').fill(local(fixture.to));
  await page.getByRole('button', { name: 'Apply filters' }).click(); await expect(page.getByRole('table', { name: 'GPS exception episodes' })).toBeVisible();
}
function local(value: string) { const date = new Date(value); return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16); }
async function login(username = process.env.E2E_ADMIN_USERNAME ?? 'admin', password = process.env.E2E_ADMIN_PASSWORD ?? 'AdminPass!2026') { const api = await request.newContext({ baseURL: backend }); const response = await api.post('/api/auth/login', { data: { username, password } }); expect(response.status(), await response.text()).toBe(200); const auth = await response.json() as Auth; await api.dispose(); return auth; }
function authorized(auth: Auth) { return request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } }); }
async function createUser(api: Awaited<ReturnType<typeof request.newContext>>, permissions: string[], kind: string) { const marker = `${kind}-${Date.now()}-${Math.random().toString(16).slice(2)}`, role = await api.post('/api/roles', { data: { name: `US55 ${marker}`, active: true, permissions } }); expect(role.status(), await role.text()).toBe(201); const roleId = (await role.json() as { id: string }).id, username = `us55-${marker}`, password = `Us55Review!${Date.now()}`; const user = await api.post('/api/users', { data: { username, email: `${username}@example.test`, password, firstName: 'US55', lastName: kind, active: true, roleIds: [roleId] } }); expect(user.status(), await user.text()).toBe(201); return login(username, password); }
async function authenticate(page: import('@playwright/test').Page, auth: Auth) { await page.addInitScript(value => { localStorage.setItem('transport.accessToken', value.accessToken); localStorage.setItem('transport.refreshToken', value.refreshToken); }, auth); }
