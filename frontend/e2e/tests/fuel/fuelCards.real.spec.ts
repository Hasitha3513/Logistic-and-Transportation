import { expect, request, test } from '@playwright/test';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const suffix = `us35-${Date.now()}`;
type Auth = { accessToken: string; refreshToken: string };
type Api = Awaited<ReturnType<typeof request.newContext>>;
type Entity = { id: string };
type Card = Entity & { alias: string; maskedIdentifier: string; status: string; version: number };
type Transaction = Entity & { providerTransactionId: string; cardId: string; localStatus: string; version: number;
  indicators: string[]; reconciledPurchaseId?: string; transactionKind: string; originalProviderTransactionId?: string;
  totalAmount: number; currency: string; providerStatus: string };

test.describe.serial('US-35 real PostgreSQL fuel-card acceptance', () => {
  let admin: Auth; let manager: Auth; let reconciler: Auth; let limited: Auth; let tenantB: Auth;
  let providerId: string; let vehicleId: string; let purchaseId: string; let card: Card; let matched: Transaction;
  const opaqueReference = `opaque-${suffix}`;

  test.beforeAll(async () => {
    admin = await login(); const api = await authorized(admin);
    manager = await createUser(api, 'manager', ['FUEL_CARD_VIEW', 'FUEL_CARD_MANAGE', 'FUEL_CARD_BLOCK', 'FUEL_CARD_IMPORT', 'FUEL_CARD_RECONCILE']);
    reconciler = await createUser(api, 'reconciler', ['FUEL_CARD_VIEW', 'FUEL_CARD_RECONCILE']);
    limited = await createUser(api, 'limited', ['FUEL_CARD_VIEW']);
    const vendor = await api.post('/api/vendors', { data: { code: `FC-${suffix}`, name: 'US-35 provider', active: true } });
    expect(vendor.status(), await vendor.text()).toBe(201); providerId = (await vendor.json() as Entity).id;
    const vehicles = entities(await expectJson(api.get('/api/vehicles'))); expect(vehicles.length).toBeGreaterThan(0); vehicleId = vehicles[0].id;
    const purchase = await api.post('/api/fuel-purchases', { data: { vendorId: providerId, fuelType: 'DIESEL',
      purchaseDate: '2026-09-04', invoiceNumber: `FC-${suffix}`, invoiceDate: '2026-09-04', quantity: 1,
      unitPrice: 300, taxRate: 0, otherCharges: 0, currencyCode: 'LKR', notes: 'US-35 reconciliation fixture' } });
    expect(purchase.status(), await purchase.text()).toBe(201); purchaseId = (await purchase.json() as Entity).id;
    const otherTenant = await api.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(otherTenant.status(), await otherTenant.text()).toBe(201); const other = await otherTenant.json() as { username: string; password: string };
    tenantB = await login(other.username, other.password); await api.dispose();
  });

  test('1/6 creates, binds, restricts and activates a masked local card in Chromium', async ({ page }) => {
    await authenticatePage(page, manager); await page.goto('/fuel/cards');
    await page.getByRole('button', { name: 'Create fuel card' }).click();
    await page.getByLabel('Provider UUID').fill(providerId); await page.getByLabel('Alias').fill(`Acceptance ${suffix}`);
    await page.getByLabel('Opaque provider reference').fill(opaqueReference);
    await page.getByLabel('Masked identifier').fill('**** 3535'); await page.getByLabel('Last four').fill('3535');
    await page.getByRole('button', { name: 'OK' }).click();
    await expect(page.getByText('**** 3535').first()).toBeVisible(); expect(await page.locator('body').innerText()).not.toContain(opaqueReference);
    const api = await authorized(manager); card = (await listCards(api)).find(value => value.alias === `Acceptance ${suffix}`)!;
    expect(card.status).toBe('DRAFT');
    const response = await api.post(`/api/v1/fuel/cards/${card.id}/bindings`, { data: { bindingType: 'VEHICLE', bindingId: vehicleId, version: card.version, reason: 'acceptance binding' } });
    expect(response.status(), await response.text()).toBe(200); card = await getCard(api, card.id);
    const restrictionResponse = await api.put(`/api/v1/fuel/cards/${card.id}/restrictions`, { data: restriction(card.version, 1000) });
    expect(restrictionResponse.status(), await restrictionResponse.text()).toBe(200); card = await getCard(api, card.id);
    const activationResponse = await api.post(`/api/v1/fuel/cards/${card.id}/activate`, { data: { version: card.version, reason: 'ready' } });
    expect(activationResponse.status(), await activationResponse.text()).toBe(200); card = await activationResponse.json() as Card; expect(card.status).toBe('ACTIVE');
    await api.dispose();
  });

  test('2/6 imports a real canonical file, deduplicates replay and evaluates retrospective indicators', async () => {
    const api = await authorized(manager);
    const valid = canonical('batch-valid', [{ id: 'transaction-valid', reference: opaqueReference, amount: 300, vehicle: vehicleId }]);
    const first = await upload(api, providerId, valid); expect(first.status(), await first.text()).toBe(200);
    const batch = await first.json() as { id: string; importedCount: number }; expect(batch.importedCount).toBe(1);
    const replay = await upload(api, providerId, valid); expect(replay.status(), await replay.text()).toBe(200);
    expect((await replay.json() as { id: string }).id).toBe(batch.id);
    const response = await api.put(`/api/v1/fuel/cards/${card.id}/restrictions`, { data: restriction(card.version, 100) });
    expect(response.status(), await response.text()).toBe(200); card = await getCard(api, card.id);
    const review = await upload(api, providerId, canonical('batch-review', [{ id: 'transaction-review', reference: opaqueReference, amount: 300, vehicle: crypto.randomUUID() }]));
    expect(review.status(), await review.text()).toBe(200);
    const transactions = await listTransactions(api); const flagged = transactions.find(value => value.providerTransactionId === `transaction-review-${suffix}`)!;
    expect(flagged.localStatus).toBe('REVIEW_REQUIRED'); expect(flagged.indicators).toEqual(expect.arrayContaining(['LIMIT_EXCEEDED', 'BINDING_MISMATCH']));
    await api.dispose();
  });

  test('3/6 enforces importer/reconciler separation and preserves provider facts through matching', async () => {
    const importerApi = await authorized(manager); matched = (await listTransactions(importerApi)).find(value => value.providerTransactionId === `transaction-valid-${suffix}`)!;
    const before = sourceFacts(matched);
    const denied = await importerApi.post(`/api/v1/fuel/card-transactions/${matched.id}/match`, { data: { purchaseId, version: matched.version, reason: 'self match denied' } });
    expect(denied.status()).toBe(400);
    const reconcileApi = await authorized(reconciler);
    const accepted = await reconcileApi.post(`/api/v1/fuel/card-transactions/${matched.id}/match`, { data: { purchaseId, version: matched.version, reason: 'independent reconciliation' } });
    expect(accepted.status(), await accepted.text()).toBe(200); matched = await accepted.json() as Transaction;
    expect(matched.localStatus).toBe('RECONCILED'); expect(matched.reconciledPurchaseId).toBe(purchaseId); expect(sourceFacts(matched)).toEqual(before);
    await importerApi.dispose(); await reconcileApi.dispose();
  });

  test('4/6 retains blocked-card facts, rejects conflicting identity and preserves a separate reversal', async () => {
    const api = await authorized(manager); card = await getCard(api, card.id);
    const blocked = await api.post(`/api/v1/fuel/cards/${card.id}/block`, { data: { version: card.version, reason: 'local security block' } });
    expect(blocked.status(), await blocked.text()).toBe(200); card = await blocked.json() as Card;
    const inactive = await upload(api, providerId, canonical('batch-inactive', [{ id: 'transaction-inactive', reference: opaqueReference, amount: 10, vehicle: vehicleId }]));
    expect(inactive.status(), await inactive.text()).toBe(200);
    expect((await listTransactions(api)).find(value => value.providerTransactionId === `transaction-inactive-${suffix}`)!.indicators).toContain('CARD_INACTIVE');
    const conflict = await upload(api, providerId, canonical('batch-conflict', [{ id: 'transaction-valid', reference: opaqueReference, amount: 301, vehicle: vehicleId }]));
    expect(conflict.status()).toBe(409);
    const reversal = await upload(api, providerId, canonical('batch-reversal', [{ id: 'transaction-reversal', reference: opaqueReference, amount: 300, vehicle: vehicleId, kind: 'REVERSAL', original: 'transaction-valid' }]));
    expect(reversal.status(), await reversal.text()).toBe(200);
    const facts = await listTransactions(api); const reversalFact = facts.find(value => value.providerTransactionId === `transaction-reversal-${suffix}`)!;
    expect(reversalFact.transactionKind).toBe('REVERSAL'); expect(reversalFact.originalProviderTransactionId).toBe(`transaction-valid-${suffix}`);
    expect(facts.find(value => value.providerTransactionId === `transaction-valid-${suffix}`)).toBeTruthy(); await api.dispose();
  });

  test('5/6 prevents tenant inference and denies limited management, import and reconciliation', async () => {
    const other = await authorized(tenantB); expect((await other.get(`/api/v1/fuel/cards/${card.id}`)).status()).toBe(404);
    expect((await other.get(`/api/v1/fuel/card-transactions/${matched.id}`)).status()).toBe(404);
    const viewer = await authorized(limited);
    expect((await viewer.post('/api/v1/fuel/cards', { data: {} })).status()).toBe(403);
    expect((await viewer.post('/api/v1/fuel/card-imports')).status()).toBe(403);
    expect((await viewer.post(`/api/v1/fuel/card-transactions/${matched.id}/reject`, { data: { version: matched.version, reason: 'denied' } })).status()).toBe(403);
    await other.dispose(); await viewer.dispose();
  });

  test('6/6 exposes only frozen routes and never exposes sensitive card or payment data', async () => {
    const api = await authorized(manager); const response = await api.get(`/api/v1/fuel/cards/${card.id}`);
    expect(response.status(), await response.text()).toBe(200); const serialized = JSON.stringify(await response.json()).toLowerCase();
    expect(serialized).not.toContain(opaqueReference.toLowerCase());
    for (const forbidden of ['providercardreference', 'pan', 'cvv', 'pin', 'balance', 'credential']) expect(serialized).not.toContain(forbidden);
    const filteredCards = await expectJson(api.get(`/api/v1/fuel/cards?status=BLOCKED&providerId=${providerId}&bindingType=VEHICLE&bindingId=${vehicleId}&expiryFrom=202601&expiryTo=203012&reviewRequired=true&sort=expiry&direction=asc`)) as Card[];
    expect(filteredCards.map(value => value.id)).toContain(card.id);
    const filteredTransactions = await expectJson(api.get(`/api/v1/fuel/card-transactions?cardId=${card.id}&providerId=${providerId}&indicator=CARD_INACTIVE&reviewRequired=true&sort=amount&direction=desc`)) as Transaction[];
    expect(filteredTransactions.some(value => value.indicators.includes('CARD_INACTIVE'))).toBe(true);
    expect([403, 405]).toContain((await api.delete(`/api/v1/fuel/cards/${card.id}`)).status());
    expect([403, 405]).toContain((await api.patch(`/api/v1/fuel/cards/${card.id}`, { data: { status: 'ACTIVE' } })).status());
    expect([403, 405]).toContain((await api.delete(`/api/v1/fuel/card-transactions/${matched.id}`)).status()); await api.dispose();
  });

  function authorized(auth: Auth) { return request.newContext({ baseURL: backend, extraHTTPHeaders: { Authorization: `Bearer ${auth.accessToken}` } }); }
  async function createUser(api: Api, label: string, permissions: string[]) {
    const role = await api.post('/api/roles', { data: { name: `US35 ${label} ${suffix}`, active: true, permissions } });
    expect(role.status(), await role.text()).toBe(201); const roleId = (await role.json() as Entity).id;
    const username = `us35-${label}-${suffix}`; const password = `Us35!${label}-${suffix}`;
    const user = await api.post('/api/users', { data: { username, email: `${username}@example.test`, password,
      firstName: 'US35', lastName: label, active: true, roleIds: [roleId] } });
    expect(user.status(), await user.text()).toBe(201); return login(username, password);
  }
  async function listCards(api: Api) { return await expectJson(api.get('/api/v1/fuel/cards?limit=100')) as Card[]; }
  async function getCard(api: Api, id: string) { return await expectJson(api.get(`/api/v1/fuel/cards/${id}`)) as Card; }
  async function listTransactions(api: Api) { return await expectJson(api.get('/api/v1/fuel/card-transactions?limit=100')) as Transaction[]; }
  async function expectJson(call: Promise<import('@playwright/test').APIResponse>) { const response = await call; expect(response.status(), await response.text()).toBe(200); return response.json(); }
  function entities(value: unknown): Entity[] { return Array.isArray(value) ? value as Entity[] : (value as { content: Entity[] }).content; }
  function restriction(version: number, max: number) { return { currency: 'LKR', maxTransactionAmount: max, maxDailyAmount: 10000,
    maxMonthlyAmount: 100000, maxDailyLitres: 1000, allowedFuelTypes: ['DIESEL'], allowedStationReferences: [], version, reason: 'acceptance limits' }; }
  function canonical(batch: string, values: Array<{ id: string; reference: string; amount: number; vehicle: string; kind?: string; original?: string }>) {
    return JSON.stringify({ schemaVersion: 'FUEL_CARD_TRANSACTIONS_V1', providerBatchId: `${batch}-${suffix}`, generatedAt: '2026-09-04T10:00:00Z',
      transactions: values.map(value => ({ providerTransactionId: `${value.id}-${suffix}`, providerCardReference: value.reference,
        transactionKind: value.kind ?? 'PURCHASE', ...(value.original ? { originalProviderTransactionId: `${value.original}-${suffix}` } : {}),
        transactionTimestamp: '2026-09-04T09:00:00Z', stationReference: 'STATION-1', fuelType: 'DIESEL', quantityLitres: 1,
        unitPrice: value.amount, totalAmount: value.amount, currency: 'LKR', providerVehicleReference: value.vehicle,
        providerStatus: value.kind === 'REVERSAL' ? 'REVERSED' : 'POSTED' })) });
  }
  async function upload(api: Api, provider: string, json: string) { return api.post(`/api/v1/fuel/card-imports?providerId=${provider}`, { multipart: {
    file: { name: 'fuel-card-transactions.json', mimeType: 'application/json', buffer: Buffer.from(json, 'utf8') } } }); }
  function sourceFacts(value: Transaction) { return { providerTransactionId: value.providerTransactionId, cardId: value.cardId,
    transactionKind: value.transactionKind, totalAmount: value.totalAmount, currency: value.currency, providerStatus: value.providerStatus }; }
  async function authenticatePage(page: import('@playwright/test').Page, auth: Auth) { await page.addInitScript(value => {
    localStorage.setItem('transport.accessToken', value.accessToken); localStorage.setItem('transport.refreshToken', value.refreshToken); }, auth); }
});

async function login(username = process.env.E2E_ADMIN_USERNAME ?? 'admin', password = process.env.E2E_ADMIN_PASSWORD ?? 'AdminPass!2026') {
  const api = await request.newContext({ baseURL: backend }); const response = await api.post('/api/auth/login', { data: { username, password } });
  expect(response.status(), await response.text()).toBe(200); const auth = await response.json() as Auth; await api.dispose(); return auth;
}
