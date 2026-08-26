import { expect, type APIRequestContext, type BrowserContext, type Page, type TestInfo } from '@playwright/test';
import { adminLogin, authenticatePage, headers, type AuthTokens, unique } from './notificationTestApi';

export type LocalOfflineOperation = {
  operationId: string;
  operationType: string;
  aggregateType: string;
  aggregateId: string;
  ownerUserId: string;
  status: 'PENDING' | 'SYNCING' | 'SYNCED' | 'FAILED' | 'CONFLICT';
  attemptCount: number;
  nextAttemptAt?: string;
  lastErrorCode?: string;
  payload: Record<string, unknown>;
  [key: string]: unknown;
};

export async function setupRealAdmin(page: Page, request: APIRequestContext) {
  const tokens = await adminLogin(request);
  await authenticatePage(page, tokens);
  return tokens;
}

export async function currentUser(request: APIRequestContext, tokens: AuthTokens) {
  const response = await request.get('/api/auth/me', { headers: headers(tokens) });
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.json() as Promise<{ id: string; username: string; permissions: string[] }>;
}

export async function createVehicle(request: APIRequestContext, tokens: AuthTokens, testInfo: TestInfo, prefix: string) {
  const registrationNumber = unique(prefix, testInfo).toUpperCase().slice(0, 30);
  const response = await request.post('/api/vehicles', { headers: headers(tokens), data: {
    registrationNumber,
    categoryId: '30000000-0000-0000-0000-000000000001',
    typeId: '31000000-0000-0000-0000-000000000001',
    manufacturer: 'E2E', model: 'Offline', manufactureYear: 2026,
    ownershipType: 'COMPANY_OWNED', operationalStatus: 'AVAILABLE',
    currentOdometerKm: 0, engineHours: 0, capacityKg: 5000, active: true,
  }});
  expect(response.status(), await response.text()).toBe(201);
  return response.json() as Promise<{ id: string; registrationNumber: string }>;
}

export async function openVehicleReadings(page: Page, vehicleId: string) {
  await page.goto(`/fleet/vehicles?vehicleId=${vehicleId}`);
  await expect(page.getByText('Vehicle Mileage & Readings')).toBeVisible({ timeout: 20_000 });
  await waitForOfflineStorageReady(page);
}

export async function closeVehicleDetails(page: Page) {
  const drawer = page.getByRole('dialog', { name: 'Vehicle registry details' });
  if (await drawer.isVisible().catch(() => false)) {
    await drawer.getByRole('button', { name: 'Close' }).click();
    await expect(drawer).toBeHidden();
  }
}

export async function captureVehicleReading(page: Page, value: number, notes: string) {
  await page.getByRole('button', { name: 'Record Reading' }).click();
  const dialog = page.getByRole('dialog', { name: 'Record Vehicle Reading' });
  await dialog.getByLabel('Reading Value').fill(String(value));
  await dialog.getByLabel('Notes').fill(notes);
  await dialog.getByRole('button', { name: 'OK' }).click();
  await expect.poll(async () => (await readOfflineOperations(page)).some(operation =>
    operation.operationType === 'VEHICLE_READING_RECORD'
      && operation.payload.notes === notes), { timeout: 15_000 }).toBe(true);
  await expect(dialog).toBeHidden();
}

export async function openTripLogs(page: Page, tripId = '60000000-0000-0000-0000-000000000006') {
  await page.goto(`/trips/${tripId}`);
  await page.getByRole('tab', { name: 'Trip Logs' }).click();
  await expect(page.getByText('En-Route Checkpoints & Operational Events')).toBeVisible({ timeout: 20_000 });
  await waitForOfflineStorageReady(page);
}

export async function waitForOfflineStorageReady(page: Page) {
  await expect.poll(() => page.evaluate(async () => {
    if (typeof indexedDB.databases !== 'function') return false;
    const databases = await indexedDB.databases();
    if (!databases.some(database => database.name === 'transport-logistics-offline' && database.version === 1)) return false;
    const request = indexedDB.open('transport-logistics-offline', 1);
    const database = await new Promise<IDBDatabase>((resolve, reject) => {
      request.onsuccess = () => resolve(request.result); request.onerror = () => reject(request.error);
    });
    const ready = database.objectStoreNames.contains('operations') && database.objectStoreNames.contains('metadata');
    if (!ready) { database.close(); return false; }
    const transaction = database.transaction('metadata', 'readonly');
    const get = transaction.objectStore('metadata').get('clientInstanceId');
    const metadata = await new Promise<{ value?: string } | undefined>((resolve, reject) => {
      get.onsuccess = () => resolve(get.result); get.onerror = () => reject(get.error);
    });
    database.close();
    return typeof metadata?.value === 'string';
  }), { timeout: 10_000 }).toBe(true);
}

export async function captureTripEvent(page: Page, family: 'CHECKPOINT' | 'DELAY' | 'INCIDENT', marker: string) {
  if (family === 'CHECKPOINT') {
    await page.getByRole('button', { name: 'Record Checkpoint' }).click();
    const dialog = page.getByRole('dialog', { name: 'Record En-Route Checkpoint' });
    await dialog.getByLabel('Checkpoint Type').click();
    await page.locator('.ant-select-dropdown:visible .ant-select-item-option', { hasText: 'Departure' })
      .evaluate((option: HTMLElement) => option.click());
    await dialog.getByLabel('Location / Waypoint Description').fill(marker);
    await dialog.getByRole('button', { name: 'Record Checkpoint' }).click();
    await expect(dialog).toBeHidden();
  } else if (family === 'DELAY') {
    await page.getByRole('button', { name: 'Record Delay' }).click();
    const dialog = page.getByRole('dialog', { name: 'Record Operational Delay' });
    await dialog.getByLabel('Delay Duration (Minutes)').fill('17');
    await dialog.getByLabel('Delay Reason').fill(marker);
    await dialog.getByRole('button', { name: 'Record Delay' }).click();
    await expect(dialog).toBeHidden();
  } else {
    await page.getByRole('button', { name: 'Record Incident' }).click();
    const dialog = page.getByRole('dialog', { name: 'Record Operational Incident' });
    await dialog.getByLabel('Incident Severity').click();
    await page.locator('.ant-select-dropdown:visible .ant-select-item-option', { hasText: 'Low' })
      .evaluate((option: HTMLElement) => option.click());
    await dialog.getByLabel('Incident Description / Cause').fill(marker);
    await dialog.getByRole('button', { name: 'Record Incident' }).click();
    await expect(dialog).toBeHidden();
  }
  await expect.poll(async () => (await readOfflineOperations(page)).some(operation => {
    if (family === 'CHECKPOINT') return operation.operationType === 'TRIP_CHECKPOINT_RECORD'
      && operation.payload.locationDescription === marker;
    if (family === 'DELAY') return operation.operationType === 'TRIP_DELAY_RECORD'
      && operation.payload.reason === marker;
    return operation.operationType === 'TRIP_INCIDENT_RECORD' && operation.payload.description === marker;
  }), { timeout: 15_000 }).toBe(true);
}

export async function readOfflineOperations(page: Page): Promise<LocalOfflineOperation[]> {
  return page.evaluate(async () => {
    const databases = typeof indexedDB.databases === 'function' ? await indexedDB.databases() : [];
    if (databases.length > 0 && !databases.some(database => database.name === 'transport-logistics-offline')) {
      return [];
    }
    const request = indexedDB.open('transport-logistics-offline', 1);
    const database = await new Promise<IDBDatabase>((resolve, reject) => {
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
    if (!database.objectStoreNames.contains('operations')) {
      database.close();
      return [];
    }
    const transaction = database.transaction('operations', 'readonly');
    const all = transaction.objectStore('operations').getAll();
    const result = await new Promise<unknown[]>((resolve, reject) => {
      all.onsuccess = () => resolve(all.result);
      all.onerror = () => reject(all.error);
    });
    database.close();
    return result;
  }) as Promise<LocalOfflineOperation[]>;
}

export async function operationFor(page: Page, aggregateId: string, type?: string) {
  const matches = (await readOfflineOperations(page)).filter((item) =>
    item.aggregateId === aggregateId && (!type || item.operationType === type));
  expect(matches.length).toBeGreaterThan(0);
  return matches.sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))[0];
}

export async function waitForOperationStatus(page: Page, operationId: string, status: LocalOfflineOperation['status']) {
  await expect.poll(async () => {
    const operation = (await readOfflineOperations(page)).find((item) => item.operationId === operationId);
    if (operation && operation.status !== status && ['SYNCED', 'FAILED', 'CONFLICT'].includes(operation.status)) {
      throw new Error(`Operation reached ${operation.status}: ${operation.lastErrorCode ?? ''} ${String(operation.lastErrorMessage ?? '')}`);
    }
    return operation?.status;
  }, { timeout: 20_000 }).toBe(status);
  return (await readOfflineOperations(page)).find((item) => item.operationId === operationId)!;
}

export async function waitForRetryScheduled(page: Page, operationId: string) {
  await expect.poll(async () => {
    const operation = (await readOfflineOperations(page)).find(item => item.operationId === operationId);
    return operation?.status === 'PENDING' && operation.attemptCount >= 1 && Boolean(operation.nextAttemptAt);
  }, { timeout: 30_000 }).toBe(true);
  return (await readOfflineOperations(page)).find(item => item.operationId === operationId)!;
}

export async function configureOutcome(request: APIRequestContext, tokens: AuthTokens, operationId: string,
  mode: 'APPLIED' | 'REJECTED' | 'CONFLICT' | 'RETRYABLE' | 'BLOCK', remainingAttempts = 1) {
  const response = await request.post('/api/e2e/offline-sync/outcomes', {
    headers: headers(tokens), data: { operationId, mode, remainingAttempts },
  });
  expect(response.status(), await response.text()).toBe(204);
}

export async function releaseOutcome(request: APIRequestContext, tokens: AuthTokens, operationId: string) {
  const response = await request.post(`/api/e2e/offline-sync/release?operationId=${operationId}`, { headers: headers(tokens) });
  expect(response.status(), await response.text()).toBe(204);
}

export async function inbox(request: APIRequestContext, tokens: AuthTokens, operationId: string) {
  const response = await request.get(`/api/e2e/offline-sync/inbox?operationId=${operationId}`, { headers: headers(tokens) });
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.json() as Promise<Record<string, unknown>>;
}

export async function resetControls(request: APIRequestContext, tokens: AuthTokens) {
  await request.delete('/api/e2e/offline-sync/outcomes', { headers: headers(tokens) });
}

export async function serverVehicleReadings(request: APIRequestContext, tokens: AuthTokens, vehicleId: string) {
  const response = await request.get(`/api/vehicles/${vehicleId}/readings?limit=100`, { headers: headers(tokens) });
  expect(response.ok(), await response.text()).toBeTruthy();
  const body = await response.json();
  return body.content as Array<{ id: string; readingValue: number; notes?: string }>;
}

export async function serverTripEvents(request: APIRequestContext, tokens: AuthTokens, tripId: string) {
  const response = await request.get(`/api/trips/${tripId}/operational-events`, { headers: headers(tokens) });
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.json() as Promise<Array<{ id: string; eventType: string; reason?: string; locationDescription?: string }>>;
}

export async function setSyncBackendUnavailable(page: Page) {
  await page.route('**/api/offline-sync/operations', route => route.abort('connectionrefused'));
}

export async function restoreSyncBackend(page: Page) {
  await page.unroute('**/api/offline-sync/operations');
}

export async function triggerOnline(page: Page) {
  await page.evaluate(() => window.dispatchEvent(new Event('online')));
}

export async function markOperationDue(page: Page, operationId: string) {
  await page.evaluate(async (id) => {
    const request = indexedDB.open('transport-logistics-offline', 1);
    const database = await new Promise<IDBDatabase>((resolve, reject) => {
      request.onsuccess = () => resolve(request.result); request.onerror = () => reject(request.error);
    });
    const transaction = database.transaction('operations', 'readwrite');
    const store = transaction.objectStore('operations');
    const get = store.get(id);
    const operation = await new Promise<Record<string, unknown>>((resolve, reject) => {
      get.onsuccess = () => resolve(get.result); get.onerror = () => reject(get.error);
    });
    operation.nextAttemptAt = new Date(0).toISOString();
    store.put(operation);
    await new Promise<void>((resolve, reject) => {
      transaction.oncomplete = () => resolve(); transaction.onerror = () => reject(transaction.error);
    });
    database.close();
  }, operationId);
}

export async function deferOperation(page: Page, operationId: string, milliseconds = 60_000) {
  await page.evaluate(async ({ id, next }) => {
    const request = indexedDB.open('transport-logistics-offline', 1);
    const database = await new Promise<IDBDatabase>((resolve, reject) => {
      request.onsuccess = () => resolve(request.result); request.onerror = () => reject(request.error);
    });
    const transaction = database.transaction('operations', 'readwrite');
    const store = transaction.objectStore('operations');
    const get = store.get(id);
    const operation = await new Promise<Record<string, unknown>>((resolve, reject) => {
      get.onsuccess = () => resolve(get.result); get.onerror = () => reject(get.error);
    });
    operation.nextAttemptAt = new Date(Date.now() + next).toISOString();
    store.put(operation);
    await new Promise<void>((resolve, reject) => {
      transaction.oncomplete = () => resolve(); transaction.onerror = () => reject(transaction.error);
    });
    database.close();
  }, { id: operationId, next: milliseconds });
}

export async function ensureOnline(context: BrowserContext) {
  await context.setOffline(false).catch(() => undefined);
}
