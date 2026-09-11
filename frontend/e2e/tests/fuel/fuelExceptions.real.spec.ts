import { expect, request, test } from '@playwright/test';
import { bunkerLedgerState, correctionAttemptState, fuelCardIndicatorState, handoffState, latestFuelCardIndicatorState,
  negativeSignalState } from '../../utils/acceptanceDb';

const backend = process.env.REAL_E2E_BACKEND_URL ?? 'http://localhost:8088';
const suffix = `us38-${Date.now()}`;
type Auth = { accessToken: string; refreshToken: string };
type Api = Awaited<ReturnType<typeof request.newContext>>;
type Entity = { id: string };
type Station = Entity & { stationType: string };
type Tank = Entity & { currentStockLiters: number };
type Price = Entity & { vendorId: string; fuelType: string; effectiveFrom: string; effectiveTo: string;
  unitPrice: number; currencyCode: string; active: boolean };
type Purchase = Entity & { quantity: number; unitPrice: number; subtotal: number; taxAmount: number;
  otherCharges: number; totalAmount: number; currencyCode: string; purchaseDate: string };
type Reading = Entity & { vehicleId: string; readingType: string; value: number; recordedAt: string;
  correctionOfReadingId?: string; correctionReason?: string };
type Issue = Entity & { voucherNumber: string; fuelType: string; quantity: number; unitPrice: number;
  totalAmount: number; issueDateTime: string; status: string };
type FuelCase = Entity & { category: string; lifecycle: string; impact: string; sourceType: string;
  sourceId: string; version: number; handoffStatus: string; summary: string };

test.describe.serial('US-38 real PostgreSQL fuel-exception acceptance', () => {
  let admin: Auth; let approver: Auth; let limited: Auth; let tenantB: Auth; let tank: Tank;
  let vehicleId: string; let driverId: string; let issueStationId: string;
  const cases: FuelCase[] = [];

  test.beforeAll(async () => {
    admin = await login(); const api = await authorized(admin);
    approver = await createUser(api, 'approver', ['FUEL_EXCEPTION_VIEW', 'FUEL_EXCEPTION_APPROVE']);
    limited = await createUser(api, 'limited', ['FUEL_EXCEPTION_VIEW']);
    const otherTenant = await api.post('/api/e2e/tenant-fixtures', { data: { suffix } });
    expect(otherTenant.status(), await otherTenant.text()).toBe(201);
    const other = await otherTenant.json() as { username: string; password: string };
    tenantB = await login(other.username, other.password);
    const stations = await json<Station[]>(api.get('/api/fuel-stations'));
    expect(stations.length).toBeGreaterThan(0);
    const externalStation = stations.find(item => item.stationType === 'EXTERNAL');
    expect(externalStation).toBeTruthy(); issueStationId = externalStation!.id;
    const vehicles = entities(await json<unknown>(api.get('/api/vehicles')));
    const drivers = entities(await json<unknown>(api.get('/api/drivers')));
    expect(vehicles.length).toBeGreaterThan(0); expect(drivers.length).toBeGreaterThan(0);
    const categories = entities(await json<unknown>(api.get('/api/vehicle-categories')));
    const types = entities(await json<unknown>(api.get('/api/vehicle-types')));
    const vehicleResponse = await api.post('/api/vehicles', { data: {
      registrationNumber: `US38-${Date.now()}`.toUpperCase().slice(0, 30), categoryId: categories[0].id,
      typeId: types[0].id, manufacturer: 'E2E', model: 'Fuel exception', manufactureYear: 2026,
      ownershipType: 'COMPANY_OWNED', operationalStatus: 'AVAILABLE', currentOdometerKm: 0,
      engineHours: 0, capacityKg: 5000, active: true } });
    expect(vehicleResponse.status(), await vehicleResponse.text()).toBe(201);
    vehicleId = (await vehicleResponse.json() as Entity).id; driverId = drivers[0].id;
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
    const recordedAt = new Date().toISOString();
    const readingResponse = await requester.post(`/api/vehicles/${vehicleId}/readings`, { data: {
      readingType: 'ODOMETER', value: 100000, recordedAt, idempotencyKey: `${suffix}-reading`,
      notes: 'US-38 immutable source reading' } });
    expect(readingResponse.status(), await readingResponse.text()).toBe(201);
    const original = await readingResponse.json() as Reading;
    let value = await createCase(requester, 'INCORRECT_READING', 'HIGH', 'Incorrect reading requires compensating review',
      { sourceType: 'VEHICLE_READING', sourceId: original.id, vehicleId });
    cases.push(value);
    value = await command<FuelCase>(requester, value, 'review', { reason: 'Verified source reading review' });
    const correctionResponse = await requester.post(`/api/v1/fuel/exceptions/${value.id}/corrections`, { data: {
      correctionType: 'VEHICLE_READING_CORRECTION', changesFinancialFact: false,
      ownerCommand: { vehicleId, readingId: original.id, correctedValue: '100001.000',
        recordedAt, reason: `[E2E_FAIL_FIRST] US-38 compensating reading correction ${suffix}` } } });
    expect(correctionResponse.status(), await correctionResponse.text()).toBe(201);
    const correction = await correctionResponse.json() as { id: string; version: number };
    const selfApproval = await requester.post(`/api/v1/fuel/exceptions/${value.id}/corrections/${correction.id}/approve`,
      { data: { version: correction.version, reason: 'Self approval must fail' } });
    expect(selfApproval.status()).toBe(400);
    const failed = await reviewer.post(`/api/v1/fuel/exceptions/${value.id}/corrections/${correction.id}/approve`,
      { data: { version: correction.version, reason: 'Controlled first owner-command attempt' } });
    expect(failed.status()).toBe(500);
    const failedDetail = await json<{ corrections: Array<{ id: string; status: string; version: number }> }>(
      requester.get(`/api/v1/fuel/exceptions/${value.id}`));
    const failedCorrection = failedDetail.corrections.find(item => item.id === correction.id)!;
    expect(failedCorrection.status).toBe('FAILED');
    const approved = await reviewer.post(`/api/v1/fuel/exceptions/${value.id}/corrections/${correction.id}/approve`,
      { data: { version: failedCorrection.version, reason: 'Retry independently approved owner command' } });
    expect(approved.status(), await approved.text()).toBe(200);
    const applied = await approved.json() as { status: string; ownerResultReference?: string };
    expect(applied.status).toBe('APPLIED'); expect(applied.ownerResultReference).toBeTruthy();
    const replay = await reviewer.post(`/api/v1/fuel/exceptions/${value.id}/corrections/${correction.id}/approve`,
      { data: { version: correction.version, reason: 'Verify successful owner command replay' } });
    expect(replay.status(), await replay.text()).toBe(200);
    const attempts = correctionAttemptState(correction.id);
    expect(attempts).toMatchObject({ distinctIdempotencyKeys: 1, successes: 1, failures: 1, noopReplays: 1 });
    console.info('OWNER COMMAND IDEMPOTENCY PASS');
    console.info('OWNER COMMAND EFFECTIVE SUCCESS COUNT 1');
    const readings = await json<{ content: Reading[] }>(requester.get(`/api/vehicles/${vehicleId}/readings?limit=100`));
    const originalAfter = readings.content.find(item => item.id === original.id);
    const compensating = readings.content.find(item => item.correctionOfReadingId === original.id);
    expect(originalAfter).toMatchObject({ id: original.id, value: original.value, recordedAt: original.recordedAt });
    expect(compensating).toMatchObject({ value: 100001, correctionOfReadingId: original.id,
      correctionReason: `[E2E_FAIL_FIRST] US-38 compensating reading correction ${suffix}` });
    await requester.dispose(); await reviewer.dispose();
  });

  test('3/6 records sudden-price and emergency-refuel cases without rewriting source facts', async () => {
    const api = await authorized(admin); const reviewer = await authorized(approver);
    const before = await json<Tank>(api.get(`/api/bunker-tanks/${tank.id}`));
    const issueResponse = await api.post('/api/fuel-issues', { data: { vehicleId, driverId, fuelType: 'AUTO_DIESEL',
      quantity: 1, unitPrice: 300, stationId: issueStationId, odometer: 100002, engineHours: 1,
      issueDateTime: new Date().toISOString(), notes: `US-38 source issue ${suffix}` } });
    expect(issueResponse.status(), await issueResponse.text()).toBe(201);
    let issue = await issueResponse.json() as Issue;
    const submittedIssue = await api.post(`/api/fuel-issues/${issue.id}/submit`);
    expect(submittedIssue.status(), await submittedIssue.text()).toBe(200); issue = await submittedIssue.json() as Issue;
    const issueFacts = immutableIssueFacts(issue);
    let issueCase = await createCase(api, 'SUSPECTED_FUEL_LOSS', 'MEDIUM',
      'Fuel issue cancellation requires owner-controlled review', { sourceType: 'FUEL_ISSUE', sourceId: issue.id });
    cases.push(issueCase); issueCase = await command<FuelCase>(api, issueCase, 'review', { reason: 'Review source Fuel Issue' });
    const issueCorrectionResponse = await api.post(`/api/v1/fuel/exceptions/${issueCase.id}/corrections`, { data: {
      correctionType: 'FUEL_ISSUE_CANCEL', changesFinancialFact: true,
      ownerCommand: { fuelIssueId: issue.id, reason: `US-38 owner cancellation ${suffix}` } } });
    expect(issueCorrectionResponse.status(), await issueCorrectionResponse.text()).toBe(201);
    const issueCorrection = await issueCorrectionResponse.json() as { id: string; version: number };
    const issueApproval = await reviewer.post(`/api/v1/fuel/exceptions/${issueCase.id}/corrections/${issueCorrection.id}/approve`,
      { data: { version: issueCorrection.version, reason: 'Independent issue cancellation approval' } });
    expect(issueApproval.status(), await issueApproval.text()).toBe(200);
    const issueAfter = await json<Issue>(api.get(`/api/fuel-issues/${issue.id}`));
    expect(issueAfter.status).toBe('CANCELLED'); expect(immutableIssueFacts(issueAfter)).toEqual(issueFacts);
    const vendorResponse = await api.post('/api/vendors', { data: {
      code: `US38-${Date.now()}`, name: `US-38 effective-price vendor ${suffix}`, active: true } });
    expect(vendorResponse.status(), await vendorResponse.text()).toBe(201);
    const vendor = await vendorResponse.json() as Entity;
    const price1Response = await api.post('/api/fuel-prices', { data: { vendorId: vendor.id, fuelType: 'PETROL',
      effectiveFrom: '2026-01-01', effectiveTo: '2026-01-31', unitPrice: 100, currencyCode: 'LKR', active: true } });
    expect(price1Response.status(), await price1Response.text()).toBe(201); const price1 = await price1Response.json() as Price;
    const purchaseResponse = await api.post('/api/fuel-purchases', { data: { vendorId: vendor.id,
      fuelType: 'PETROL', purchaseDate: '2026-01-15', invoiceNumber: `US38-${suffix}`, invoiceDate: '2026-01-15',
      quantity: 10, unitPrice: 100, taxRate: 0, otherCharges: 0, currencyCode: 'LKR', notes: 'Historical P1 purchase' } });
    expect(purchaseResponse.status(), await purchaseResponse.text()).toBe(201);
    const purchaseBefore = await purchaseResponse.json() as Purchase;
    let priceCase = await createCase(api, 'SUDDEN_PRICE_CHANGE', 'HIGH',
      'Sudden price change requires effective-dated review', { sourceType: 'FUEL_PRICE', sourceId: price1.id });
    cases.push(priceCase); priceCase = await command<FuelCase>(api, priceCase, 'review', { reason: 'Review P1 price' });
    const correctionResponse = await api.post(`/api/v1/fuel/exceptions/${priceCase.id}/corrections`, { data: {
      correctionType: 'FUEL_PRICE_EFFECTIVE_DATED', changesFinancialFact: true, ownerCommand: {
        vendorId: vendor.id, fuelType: 'PETROL', effectiveFrom: '2026-02-01', effectiveTo: '2026-02-28',
        unitPrice: '110', currencyCode: 'LKR', reason: 'Approved effective-dated P2' } } });
    expect(correctionResponse.status(), await correctionResponse.text()).toBe(201);
    const correction = await correctionResponse.json() as { id: string; version: number };
    const approved = await reviewer.post(`/api/v1/fuel/exceptions/${priceCase.id}/corrections/${correction.id}/approve`,
      { data: { version: correction.version, reason: 'Independent P2 approval' } });
    expect(approved.status(), await approved.text()).toBe(200);
    const prices = await json<Price[]>(api.get(`/api/fuel-prices?vendorId=${vendor.id}&fuelType=PETROL`));
    const persistedP1 = prices.find(value => value.id === price1.id);
    const price2 = prices.find(value => value.effectiveFrom === '2026-02-01');
    expect(persistedP1).toMatchObject({ id: price1.id, vendorId: price1.vendorId, fuelType: price1.fuelType,
      effectiveFrom: price1.effectiveFrom, effectiveTo: price1.effectiveTo, unitPrice: price1.unitPrice,
      currencyCode: price1.currencyCode, active: price1.active });
    expect(price2).toBeTruthy(); expect(price2?.unitPrice).toBe(110);
    const purchaseAfter = await json<Purchase>(api.get(`/api/fuel-purchases/${purchaseBefore.id}`));
    expect(purchaseAfter).toMatchObject({ id: purchaseBefore.id, quantity: purchaseBefore.quantity,
      unitPrice: purchaseBefore.unitPrice, subtotal: purchaseBefore.subtotal, taxAmount: purchaseBefore.taxAmount,
      otherCharges: purchaseBefore.otherCharges, totalAmount: purchaseBefore.totalAmount,
      currencyCode: purchaseBefore.currencyCode, purchaseDate: purchaseBefore.purchaseDate });
    cases.push(await createCase(api, 'EMERGENCY_REFUEL', 'HIGH', 'Emergency refuel recorded retrospectively for review', {
      vehicleId, driverId, occurredAt: new Date().toISOString() }));
    const missing = await api.post('/api/v1/fuel/exceptions', { data: { category: 'EMERGENCY_REFUEL', impact: 'HIGH',
      sourceType: 'BUNKER_TANK', sourceId: tank.id, summary: `Missing emergency reference ${suffix}`,
      vehicleId: crypto.randomUUID(), driverId: crypto.randomUUID(), occurredAt: new Date().toISOString() } });
    expect(missing.status()).toBe(404);
    const after = await json<Tank>(api.get(`/api/bunker-tanks/${tank.id}`));
    expect(after.currentStockLiters).toBe(before.currentStockLiters); await api.dispose(); await reviewer.dispose();
  });

  test('4/6 records a fuel-card policy deviation while preserving US-35 command separation', async () => {
    const api = await authorized(admin);
    const indicatorBefore = latestFuelCardIndicatorState();
    cases.push(await createCase(api, 'FUEL_CARD_POLICY_DEVIATION', 'MEDIUM',
      `US-35 ${indicatorBefore.code} indicator requires independent review`,
      { sourceType: 'FUEL_CARD_INDICATOR', sourceId: indicatorBefore.indicatorId }));
    expect(fuelCardIndicatorState(indicatorBefore.indicatorId)).toEqual(indicatorBefore);
    const forbidden = await api.patch(`/api/v1/fuel/card-transactions/${crypto.randomUUID()}`, { data: { localStatus: 'RECONCILED' } });
    expect([403, 405]).toContain(forbidden.status()); await api.dispose();
  });

  test('5/6 rejects negative bunker stock, keeps stock and ledger unchanged, and creates one case', async () => {
    const api = await authorized(admin);
    const before = await json<Tank>(api.get(`/api/bunker-tanks/${tank.id}`));
    const movementsBefore = await json<{ totalElements: number }>(api.get(`/api/bunker-tanks/${tank.id}/movements?limit=1`));
    const ledgerBefore = bunkerLedgerState(tank.id);
    const rejected = await api.post(`/api/bunker-tanks/${tank.id}/adjustments`, { data: {
      quantityDeltaLiters: String(-(Number(before.currentStockLiters) + 1)), reason: 'US-38 negative balance rejection' } });
    expect(rejected.status()).toBe(400);
    const after = await json<Tank>(api.get(`/api/bunker-tanks/${tank.id}`));
    const movementsAfter = await json<{ totalElements: number }>(api.get(`/api/bunker-tanks/${tank.id}/movements?limit=1`));
    expect(after.currentStockLiters).toBe(before.currentStockLiters);
    expect(movementsAfter.totalElements).toBe(movementsBefore.totalElements);
    const found = await json<FuelCase[]>(api.get(`/api/v1/fuel/exceptions?category=NEGATIVE_BUNKER_BALANCE&tankId=${tank.id}`));
    expect(found).toHaveLength(1);
    const firstSignal = negativeSignalState(found[0].id);
    expect(firstSignal.sourceEventId).toBeTruthy(); expect(firstSignal.activeCaseCount).toBe(1);
    const replay = await api.post(`/api/bunker-tanks/${tank.id}/adjustments`, { data: {
      quantityDeltaLiters: String(-(Number(before.currentStockLiters) + 1)), reason: 'US-38 negative balance rejection' } });
    expect(replay.status()).toBe(400);
    const replayed = await json<FuelCase[]>(api.get(`/api/v1/fuel/exceptions?category=NEGATIVE_BUNKER_BALANCE&tankId=${tank.id}`));
    expect(replayed).toHaveLength(1);
    const secondSignal = negativeSignalState(found[0].id);
    expect(secondSignal.sourceEventId).toBe(firstSignal.sourceEventId);
    expect(secondSignal.activeCaseCount).toBe(1);
    const finalTank = await json<Tank>(api.get(`/api/bunker-tanks/${tank.id}`));
    const finalMovements = await json<{ totalElements: number }>(api.get(`/api/bunker-tanks/${tank.id}/movements?limit=1`));
    expect(finalTank.currentStockLiters).toBe(before.currentStockLiters);
    expect(finalMovements.totalElements).toBe(movementsBefore.totalElements);
    const rejectedLedger = bunkerLedgerState(tank.id);
    expect(rejectedLedger).toEqual(ledgerBefore);
    const valid = await api.post(`/api/bunker-tanks/${tank.id}/adjustments`, { data: {
      quantityDeltaLiters: '1.000', reason: 'US-38 post-rejection sequence proof' } });
    expect(valid.status(), await valid.text()).toBe(201);
    const validLedger = bunkerLedgerState(tank.id);
    expect(validLedger.latestSequence).toBe(ledgerBefore.latestSequence + 1);
    expect(validLedger.movementCount).toBe(ledgerBefore.movementCount + 1);
    expect(Number(validLedger.stock)).toBeCloseTo(Number(ledgerBefore.stock) + 1, 3);
    console.info('SOURCE EVENT ID STABLE ACROSS RETRY PASS');
    console.info('ACTIVE NEGATIVE-BALANCE CASE COUNT 1');
    console.info('NEGATIVE-BALANCE LEDGER UNCHANGED PASS');
    cases.push(found[0]); await api.dispose();
  });

  test('6/6 performs durable critical handoff and enforces tenant and command RBAC', async () => {
    const api = await authorized(admin);
    let critical = await createCase(api, 'SUSPECTED_FUEL_LOSS', 'CRITICAL',
      'Critical unexplained variance requiring Operations coordination', { sourceType: 'REJECTED_BUNKER_COMMAND' });
    critical = await command<FuelCase>(api, critical, 'escalate', { reason: '[E2E_FAIL_FIRST] Cross-module coordination required' });
    expect(critical.handoffStatus).toBe('FAILED');
    const failedHandoff = handoffState(critical.id);
    expect(failedHandoff.status).toBe('FAILED'); expect(failedHandoff.attemptCount).toBe(1);
    critical = await command<FuelCase>(api, critical, 'escalate', { reason: 'Retry durable Operations handoff' });
    expect(critical.handoffStatus).toBe('PUBLISHED');
    await expect.poll(async () => {
      const operations = await json<{ content: Array<{ sourceModule: string; sourceId: string }> }>(
        api.get('/api/v1/operational-exceptions?sourceModule=FUEL'));
      return operations.content.filter(value => value.sourceId === critical.id).length;
    }, { timeout: 15_000 }).toBe(1);
    const publishedHandoff = handoffState(critical.id);
    expect(publishedHandoff.handoffId).toBe(failedHandoff.handoffId);
    expect(publishedHandoff.eventId).toBe(failedHandoff.eventId);
    expect(publishedHandoff.status).toBe('PUBLISHED'); expect(publishedHandoff.attemptCount).toBe(2);
    expect(publishedHandoff.operationsCaseCount).toBe(1);
    console.info('HANDOFF EVENT ID STABLE ACROSS RETRY PASS');
    console.info('OPERATIONS CASE COUNT 1');
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
function immutableIssueFacts(value: Issue) {
  return { id: value.id, voucherNumber: value.voucherNumber, fuelType: value.fuelType, quantity: value.quantity,
    unitPrice: value.unitPrice, totalAmount: value.totalAmount, issueDateTime: value.issueDateTime };
}
