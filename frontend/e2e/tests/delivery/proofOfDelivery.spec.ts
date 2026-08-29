import { expect, test, type Page } from '@playwright/test';
import { adminLogin, authenticatePage } from '../../helpers/notificationTestApi';

async function createReadyDelivery(page: Page) {
  const customers = page.waitForResponse(res => res.url().includes('/customers') && res.status() === 200);
  const locations = page.waitForResponse(res => res.url().includes('/locations') && res.status() === 200);
  await page.goto('/deliveries/new');
  await Promise.all([customers, locations]);
  const select = async (label: string, option: string) => {
    await page.getByLabel(label).first().locator('.ant-select-selector').click();
    const box = page.getByRole('combobox', { name: label }); await box.fill(option); await box.press('ArrowDown'); await box.press('Enter');
  };
  await select('Customer', 'Acme Distribution'); await select('Origin', 'Colombo'); await select('Destination', 'Kandy');
  await page.getByLabel('Window start').fill('2027-03-01 08:00:00');
  await page.getByLabel('Window start').blur();
  await page.getByLabel('Window end').fill('2027-03-01 12:00:00');
  await page.getByLabel('Window end').blur();
  await page.getByLabel('Instructions').fill('US-57 browser acceptance delivery');
  await page.getByRole('button', { name: 'Save delivery order' }).click();
  await expect(page).toHaveURL(/\/deliveries\/[0-9a-f-]+$/);
  await page.getByRole('button', { name: 'Validate readiness' }).click();
  await expect(page.getByText('READY FOR ASSIGNMENT').first()).toBeVisible();
  return (await page.getByRole('heading', { name: /DEL-\d{4}-\d{6}/ }).textContent())!.trim();
}

test.describe('US-57 online Proof of Delivery', () => {
  test.beforeEach(async ({ page, request }) => { const admin = await adminLogin(request); await authenticatePage(page, admin); });

  test('E2E-MVP13-POD-001: barcode proof finalizes and completes Delivery', async ({ page }) => {
    const number = await createReadyDelivery(page);
    await page.getByRole('button', { name: 'Start POD' }).click();
    await page.getByLabel('Delivery barcode').fill(number);
    await page.getByRole('button', { name: /Add barcode/ }).click();
    await expect(page.getByText('BARCODE', { exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Finalize POD' }).click();
    await expect(page.getByText('FINALIZED')).toBeVisible();
    await expect(page.getByText('DELIVERED', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('Not finalized')).not.toBeVisible();
  });

  test('E2E-MVP13-POD-002: no primary evidence is rejected', async ({ page }) => {
    await createReadyDelivery(page); await page.getByRole('button', { name: 'Start POD' }).click();
    await page.getByRole('button', { name: 'Finalize POD' }).click();
    await expect(page.getByText(/At least one signature, photo or barcode is required/i)).toBeVisible();
    await expect(page.getByText('READY FOR ASSIGNMENT').first()).toBeVisible();
  });

  test('E2E-MVP13-POD-003: mismatched barcode is rejected', async ({ page }) => {
    await createReadyDelivery(page); await page.getByRole('button', { name: 'Start POD' }).click();
    await page.getByLabel('Delivery barcode').fill('DEL-2027-999999'); await page.getByRole('button', { name: /Add barcode/ }).click();
    await expect(page.getByText(/Barcode does not match the Delivery Order number/i)).toBeVisible();
    await expect(page.getByText('No signature, photo or barcode evidence yet')).toBeVisible();
  });
});
