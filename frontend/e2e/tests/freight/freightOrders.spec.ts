import { expect, test, type APIRequestContext } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { adminLogin, authenticatePage, headers, provisionUser, type AuthTokens } from '../../helpers/notificationTestApi';

const customerId = '10000000-0000-0000-0000-000000000001';
const originLocationId = '20000000-0000-0000-0000-000000000001';
const destinationLocationId = '20000000-0000-0000-0000-000000000002';
const payload = (marker: string) => ({ customerId, originLocationId, destinationLocationId,
  requestedPickupAt: '2027-02-01T08:00:00Z', requestedDeliveryAt: '2027-02-02T08:00:00Z',
  serviceLevel: `SLA_${marker}`, priority: 'HIGH', specialHandlingInstructions: `Keep dry ${marker}`,
  lines: [{ description: `Pallets ${marker}`, quantity: 3 }] });

async function createOrder(api: APIRequestContext, tokens: AuthTokens, marker: string) {
  const response = await api.post('/api/v1/freight/orders', { headers: headers(tokens), data: payload(marker) });
  expect(response.status(), await response.text()).toBe(201); return response.json();
}

test.describe('US-24 freight order management', () => {
  test('E2E-P2-FRT-001: create Freight Order with SLA, priority, special handling and shipment lines', async ({ page, request }) => {
    const admin = await adminLogin(request); await authenticatePage(page, admin); await page.goto('/freight/orders/new');
    const select = async (label: string, option: string) => {
      await page.getByLabel(label).first().locator('.ant-select-selector').click();
      const combobox = page.getByRole('combobox', { name: label });
      await combobox.fill(option); await combobox.press('ArrowDown'); await combobox.press('Enter');
    };
    await select('Customer', 'Acme Distribution'); await select('Origin', 'Colombo'); await select('Destination', 'Kandy');
    await page.getByLabel('Requested pickup').fill('2027-02-01 08:00:00'); await page.getByLabel('Requested pickup').press('Enter');
    await page.getByLabel('Requested delivery').fill('2027-02-02 08:00:00'); await page.getByLabel('Requested delivery').press('Enter');
    const marker = randomUUID().slice(0, 8); await page.getByLabel('Service level code').fill(`SLA_${marker}`);
    await page.getByLabel('Priority code').fill('HIGH'); await page.getByLabel('Special handling instructions').fill(`Keep dry ${marker}`);
    await page.getByLabel('Line 1 description').fill(`Pallets ${marker}`); await page.getByLabel('Line 1 quantity').fill('3');
    await page.getByRole('button', { name: 'Save freight order' }).click();
    await expect(page).toHaveURL(/\/freight\/orders\/[0-9a-f-]+$/); await expect(page.getByText(`Keep dry ${marker}`)).toBeVisible();
    await expect(page.getByText(new RegExp(`Pallets ${marker}`))).toBeVisible();
  });

  test('E2E-P2-FRT-002: view Freight Order details', async ({ page, request }) => {
    const admin = await adminLogin(request); const order = await createOrder(request, admin, randomUUID().slice(0, 8));
    await authenticatePage(page, admin); await page.goto(`/freight/orders/${order.id}`);
    await expect(page.getByRole('heading', { name: order.orderNumber })).toBeVisible(); await expect(page.getByText(order.specialHandlingInstructions)).toBeVisible();
    await expect(page.getByText(new RegExp(order.lines[0].description))).toBeVisible();
  });

  test('E2E-P2-FRT-003: update Freight Order using optimistic version', async ({ page, request }) => {
    const admin = await adminLogin(request); const order = await createOrder(request, admin, randomUUID().slice(0, 8));
    await authenticatePage(page, admin); await page.goto(`/freight/orders/${order.id}/edit`);
    await expect(page.getByLabel('Service level code')).toHaveValue(order.serviceLevel);
    await page.getByLabel('Priority code').fill('URGENT'); await page.getByLabel('Special handling instructions').fill('Updated handling instruction');
    await page.getByRole('button', { name: 'Save freight order' }).click(); await expect(page).toHaveURL(`/freight/orders/${order.id}`);
    await expect(page.getByText('Updated handling instruction')).toBeVisible(); await expect(page.getByText('Urgent')).toBeVisible();
  });

  test('E2E-P2-FRT-004: validate required fields and shipment-line quantity', async ({ page, request }) => {
    const admin = await adminLogin(request); await authenticatePage(page, admin); await page.goto('/freight/orders/new');
    await page.getByRole('button', { name: 'Save freight order' }).click();
    await expect(page.getByText('Customer is required')).toBeVisible(); await expect(page.getByText('Description is required')).toBeVisible();
    const invalid = await request.post('/api/v1/freight/orders', { headers: headers(admin), data: { ...payload(randomUUID().slice(0, 8)), lines: [{ description: 'Invalid quantity', quantity: 0 }] } });
    expect(invalid.status()).toBe(400); expect(JSON.stringify(await invalid.json())).toMatch(/greater than( or equal to)?/i);
  });

  test('E2E-P2-FRT-005: view-only user cannot create or update Freight Order', async ({ page, request }, testInfo) => {
    const admin = await adminLogin(request); const order = await createOrder(request, admin, randomUUID().slice(0, 8));
    const viewer = await provisionUser(request, admin, `frtview-${testInfo.project.name}-${randomUUID().slice(0, 6)}`, ['FREIGHT_ORDER_VIEW']);
    await authenticatePage(page, viewer.tokens); await page.goto('/freight/orders'); await expect(page.getByRole('button', { name: 'New freight order' })).toHaveCount(0);
    await page.goto(`/freight/orders/${order.id}`); await expect(page.getByRole('button', { name: 'Edit order' })).toHaveCount(0);
    await page.goto(`/freight/orders/${order.id}/edit`); await expect(page).toHaveURL(`/freight/orders/${order.id}`);
  });

  test('E2E-P2-FRT-006: direct unauthorized mutation returns 403', async ({ request }, testInfo) => {
    const admin = await adminLogin(request); const viewer = await provisionUser(request, admin,
      `frtnone-${testInfo.project.name}-${randomUUID().slice(0, 6)}`, ['FREIGHT_ORDER_VIEW']);
    const forbidden = await request.post('/api/v1/freight/orders', { headers: headers(viewer.tokens), data: payload(randomUUID().slice(0, 8)) });
    expect(forbidden.status()).toBe(403); const unauthenticated = await request.post('/api/v1/freight/orders', { data: payload(randomUUID().slice(0, 8)) });
    expect(unauthenticated.status()).toBe(401);
  });
});
