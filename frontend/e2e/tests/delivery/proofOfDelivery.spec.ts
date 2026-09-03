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
  test.beforeEach(async ({ page, request }) => {
    page.on('response', async (response) => {
      if (response.url().includes('/proof') && response.status() >= 400) {
        console.error('POD API ERROR:', response.status(), await response.text());
      }
    });
    const admin = await adminLogin(request);
    await authenticatePage(page, admin);
  });

  test('E2E-MVP13-POD-001: barcode proof finalizes and completes Delivery', async ({ page }) => {
    const number = await createReadyDelivery(page);
    const draftRes = page.waitForResponse(res => res.url().includes('/proof') && res.request().method() === 'POST' && res.status() === 201);
    await page.getByRole('button', { name: 'Start POD' }).click();
    await draftRes;

    const evidenceRes = page.waitForResponse(res => res.url().includes('/evidence') && res.request().method() === 'POST' && res.status() === 201);
    await page.getByLabel('Delivery barcode').fill(number);
    await page.getByLabel('Delivery barcode').press('Enter');
    await evidenceRes;

    await expect(page.getByText('BARCODE', { exact: true })).toBeVisible();
    const finalizeRes = page.waitForResponse(res => res.url().includes('/finalize') && res.status() === 200);
    await page.getByRole('button', { name: 'Finalize POD' }).click();
    await finalizeRes;

    await expect(page.getByText('FINALIZED', { exact: true })).toBeVisible();
    await expect(page.getByText('DELIVERED', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('Not finalized')).not.toBeVisible();
  });

  test('E2E-MVP13-POD-002: no primary evidence is rejected', async ({ page }) => {
    await createReadyDelivery(page);
    const draftRes = page.waitForResponse(res => res.url().includes('/proof') && res.request().method() === 'POST' && res.status() === 201);
    await page.getByRole('button', { name: 'Start POD' }).click();
    await draftRes;

    await page.getByRole('button', { name: 'Finalize POD' }).click();
    await expect(page.getByText(/At least one signature, photo, or matching barcode is required|At least one signature, photo or barcode is required/i)).toBeVisible();
    await expect(page.getByText('READY FOR ASSIGNMENT').first()).toBeVisible();
  });

  test('E2E-MVP13-POD-003: mismatched barcode is rejected', async ({ page }) => {
    await createReadyDelivery(page);
    const draftRes = page.waitForResponse(res => res.url().includes('/proof') && res.request().method() === 'POST' && res.status() === 201);
    await page.getByRole('button', { name: 'Start POD' }).click();
    await draftRes;

    const evidenceErr = page.waitForResponse(res => res.url().includes('/evidence') && res.status() >= 400);
    await page.getByLabel('Delivery barcode').fill('DEL-2027-999999');
    await page.getByLabel('Delivery barcode').press('Enter');
    await evidenceErr;

    await expect(page.getByText(/Barcode does not match the Delivery Order number/i)).toBeVisible();
    await expect(page.getByText('No signature, photo or barcode evidence yet')).toBeVisible();
  });

  test('E2E-MVP13-POD-004: finalized POD is immutable (read-only and no mutation controls)', async ({ page }) => {
    const number = await createReadyDelivery(page);
    const draftRes = page.waitForResponse(res => res.url().includes('/proof') && res.request().method() === 'POST' && res.status() === 201);
    await page.getByRole('button', { name: 'Start POD' }).click();
    await draftRes;

    const evidenceRes = page.waitForResponse(res => res.url().includes('/evidence') && res.status() === 201);
    await page.getByLabel('Delivery barcode').fill(number);
    await page.getByLabel('Delivery barcode').press('Enter');
    await evidenceRes;

    await expect(page.getByText('BARCODE', { exact: true })).toBeVisible();
    const finalizeRes = page.waitForResponse(res => res.url().includes('/finalize') && res.status() === 200);
    await page.getByRole('button', { name: 'Finalize POD' }).click();
    await finalizeRes;

    await expect(page.getByText('FINALIZED', { exact: true })).toBeVisible();

    // Verify mutating controls are no longer rendered
    await expect(page.getByRole('button', { name: 'Finalize POD' })).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Start POD' })).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Add barcode' })).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Save signature' })).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Delete' })).not.toBeVisible();
  });
});
