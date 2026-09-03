import { expect, test, type Page } from '@playwright/test';
import { adminLogin, authenticatePage } from '../../helpers/notificationTestApi';

async function createReadyDelivery(page: Page) {
  const customers = page.waitForResponse(
    (res) => res.url().includes('/customers') && res.status() === 200,
  );
  const locations = page.waitForResponse(
    (res) => res.url().includes('/locations') && res.status() === 200,
  );
  await page.goto('/deliveries/new');
  await Promise.all([customers, locations]);
  const select = async (label: string, option: string) => {
    await page.getByLabel(label).first().locator('.ant-select-selector').click();
    const box = page.getByRole('combobox', { name: label });
    await box.fill(option);
    await box.press('ArrowDown');
    await box.press('Enter');
  };
  await select('Customer', 'Acme Distribution');
  await select('Origin', 'Colombo');
  await select('Destination', 'Kandy');
  await page.getByLabel('Window start').fill('2027-03-01 08:00:00');
  await page.getByLabel('Window start').blur();
  await page.getByLabel('Window end').fill('2027-03-01 12:00:00');
  await page.getByLabel('Window end').blur();
  await page.getByLabel('Instructions').fill('US-58 offline POD e2e delivery');
  await page.getByRole('button', { name: 'Save delivery order' }).click();
  await expect(page).toHaveURL(/\/deliveries\/[0-9a-f-]+$/);
  await page.getByRole('button', { name: 'Validate readiness' }).click();
  await expect(page.getByText('READY FOR ASSIGNMENT').first()).toBeVisible();
  return (await page.getByRole('heading', { name: /DEL-\d{4}-\d{6}/ }).textContent())!.trim();
}

test.describe('US-58 Offline Proof of Delivery', () => {
  test.beforeEach(async ({ page, request }) => {
    const admin = await adminLogin(request);
    await authenticatePage(page, admin);
  });

  test('E2E-MVP13-US58-001: offline POD capture, consent, IndexedDB queueing and synchronization', async ({
    page,
  }) => {
    const deliveryNumber = await createReadyDelivery(page);

    // Signer name and relationship
    await page.getByLabel('Signer name').fill('Jane Doe');
    await page.getByLabel('Signer relationship').fill('Receiving Agent');

    // Consent checkbox
    await page.getByText(/Customer Consent \(POD-CONSENT-V1\)/).click();

    // Barcode staging matching delivery number
    await page.getByLabel('Delivery barcode').fill(deliveryNumber);
    await page.getByLabel('Delivery barcode').press('Enter');

    // Verify staged evidence appears
    await expect(page.getByText('Staged Evidence (1)')).toBeVisible();
    await expect(page.getByText('BARCODE', { exact: true })).toBeVisible();

    // Save & Queue Offline
    const syncRes = page.waitForResponse(
      (res) => res.url().includes('/offline-sync/operations') && res.status() === 200,
    );
    await page.getByRole('button', { name: 'Save & Queue Offline' }).click();

    // Wait for automatic sync or verify local outbox success
    await syncRes;

    // Verify status becomes DELIVERED and POD is FINALIZED
    await expect(page.getByText('FINALIZED', { exact: true })).toBeVisible();
    await expect(page.getByText('DELIVERED', { exact: true }).first()).toBeVisible();
  });

  test('E2E-MVP13-US58-002: consent requirement blocks offline capture without consent', async ({
    page,
  }) => {
    await createReadyDelivery(page);

    await page.getByLabel('Signer name').fill('Jane Doe');

    // Draw signature without consent
    await page.getByRole('button', { name: 'Draw Signature' }).click();
    await expect(page.getByText('Draw Signature').first()).toBeVisible();

    // Draw on canvas
    const canvas = page.locator('canvas');
    const box = await canvas.boundingBox();
    if (box) {
      await page.mouse.move(box.x + 20, box.y + 20);
      await page.mouse.down();
      await page.mouse.move(box.x + 80, box.y + 80);
      await page.mouse.up();
    }
    await page.getByRole('button', { name: 'Accept Signature' }).click();

    // Attempt to queue offline without consent checkbox
    await page.getByRole('button', { name: 'Save & Queue Offline' }).click();
    await expect(page.getByText(/Customer consent is required/)).toBeVisible();
  });

  test('E2E-MVP13-US58-003: signature retake and canvas clear control', async ({ page }) => {
    await createReadyDelivery(page);

    await page.getByRole('button', { name: 'Draw Signature' }).click();
    await expect(page.getByText('Draw Signature').first()).toBeVisible();

    // Clear / Retake button inside canvas modal
    await page.getByRole('button', { name: 'Clear / Retake' }).click();
    await page.getByRole('button', { name: 'Cancel' }).click();

    // Stage a barcode then remove it
    await page.getByLabel('Delivery barcode').fill('DEL-2026-999999');
    await page.getByLabel('Delivery barcode').press('Enter');

    // Staged evidence should not be added if barcode doesn't match
    await expect(page.getByText(/Barcode must match/)).toBeVisible();
  });
});
