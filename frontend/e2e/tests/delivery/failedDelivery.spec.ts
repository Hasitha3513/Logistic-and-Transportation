import { expect, test, type Page } from '@playwright/test';
import { adminLogin, authenticatePage } from '../../helpers/notificationTestApi';

async function createReadyDelivery(page: Page) {
  const customers = page.waitForResponse(res => res.url().includes('/customers') && res.status() === 200);
  const locations = page.waitForResponse(res => res.url().includes('/locations') && res.status() === 200);
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
  await page.getByLabel('Instructions').fill('US-59 Failed delivery acceptance order');
  await page.getByRole('button', { name: 'Save delivery order' }).click();
  await expect(page).toHaveURL(/\/deliveries\/[0-9a-f-]+$/);
  await page.getByRole('button', { name: 'Validate readiness' }).click();
  await expect(page.getByText('READY FOR ASSIGNMENT').first()).toBeVisible();
  return (await page.getByRole('heading', { name: /DEL-\d{4}-\d{6}/ }).textContent())!.trim();
}

test.describe('US-59 Manage Failed Deliveries E2E', () => {
  test.beforeEach(async ({ page, request }) => {
    page.on('response', async (response) => {
      if (response.url().includes('/failed-attempt') || response.url().includes('/deliveries')) {
        if (response.status() >= 400) {
          console.error('FAILED DELIVERY API ERROR:', response.status(), await response.text());
        }
      }
    });
    const admin = await adminLogin(request);
    await authenticatePage(page, admin);
  });

  test('SCENARIO A: Record redelivery-eligible failure transitions order to FAILED_ATTEMPT', async ({ page }) => {
    await createReadyDelivery(page);

    // Select CUSTOMER_UNAVAILABLE
    await page.locator('[data-testid="select-failure-reason"]').click();
    await page.getByTitle('Customer Unavailable').click();

    // Add optional notes
    await page.locator('[data-testid="input-attempt-notes"]').fill('Customer was out of town until tomorrow');

    // Submit failed attempt
    const submitRes = page.waitForResponse(res => res.url().includes('/failed-attempt') && res.status() === 200);
    await page.locator('[data-testid="btn-submit-failed-attempt"]').click();
    await submitRes;

    // Verify order status updated to FAILED ATTEMPT
    await expect(page.getByText('FAILED ATTEMPT').first()).toBeVisible();
    await expect(page.getByText('Attempt #1')).toBeVisible();
    await expect(page.getByText('REDELIVERY_ELIGIBLE')).toBeVisible();
    await expect(page.getByText('Customer was out of town until tomorrow')).toBeVisible();
  });

  test('SCENARIO B: Record CUSTOMER_REFUSED failure transitions order to RETURN_TO_BASE', async ({ page }) => {
    await createReadyDelivery(page);

    // Select CUSTOMER_REFUSED
    await page.locator('[data-testid="select-failure-reason"]').click();
    await page.getByTitle('Customer Refused').click();

    // Required notes >= 5 chars
    await page.locator('[data-testid="input-attempt-notes"]').fill('Customer refused parcel stating duplicate order');

    const submitRes = page.waitForResponse(res => res.url().includes('/failed-attempt') && res.status() === 200);
    await page.locator('[data-testid="btn-submit-failed-attempt"]').click();
    await submitRes;

    // Verify order status updated to RETURN TO BASE
    await expect(page.getByText('RETURN TO BASE').first()).toBeVisible();
    await expect(page.getByText('RETURN_TO_BASE_REQUIRED')).toBeVisible();
    await expect(page.getByText('Return to Base Active')).toBeVisible();
  });

  test('SCENARIO C: Record DAMAGED_CARGO failure triggers escalation', async ({ page }) => {
    await createReadyDelivery(page);

    // Select DAMAGED_CARGO
    await page.locator('[data-testid="select-failure-reason"]').click();
    await page.getByTitle('Damaged Cargo').click();

    await page.locator('[data-testid="input-attempt-notes"]').fill('Outer container crushed during transit');

    const submitRes = page.waitForResponse(res => res.url().includes('/failed-attempt') && res.status() === 200);
    await page.locator('[data-testid="btn-submit-failed-attempt"]').click();
    await submitRes;

    // Verify order status is ESCALATED
    await expect(page.getByText('ESCALATED').first()).toBeVisible();
    await expect(page.getByText('Operational Escalation Active')).toBeVisible();
    await expect(page.getByText('Outer container crushed during transit').first()).toBeVisible();
  });

  test('SCENARIO D: Validation rejects short notes on CUSTOMER_REFUSED', async ({ page }) => {
    await createReadyDelivery(page);

    await page.locator('[data-testid="select-failure-reason"]').click();
    await page.getByTitle('Customer Refused').click();

    await page.locator('[data-testid="input-attempt-notes"]').fill('No');
    await page.locator('[data-testid="btn-submit-failed-attempt"]').click();

    await expect(page.getByText('Notes must be at least 5 characters for Customer Refused')).toBeVisible();
    await expect(page.getByText('READY FOR ASSIGNMENT').first()).toBeVisible();
  });

  test('SCENARIO E: Delivered orders disable failed delivery actions', async ({ page }) => {
    const number = await createReadyDelivery(page);

    // Finalize POD to reach DELIVERED status
    const draftRes = page.waitForResponse(res => res.url().includes('/proof') && res.request().method() === 'POST' && res.status() === 201);
    await page.getByRole('button', { name: 'Start POD' }).click();
    await draftRes;

    const evidenceRes = page.waitForResponse(res => res.url().includes('/evidence') && res.request().method() === 'POST' && res.status() === 201);
    await page.getByLabel('Delivery barcode').fill(number);
    await page.getByLabel('Delivery barcode').press('Enter');
    await evidenceRes;

    const finalizeRes = page.waitForResponse(res => res.url().includes('/finalize') && res.status() === 200);
    await page.getByRole('button', { name: 'Finalize POD' }).click();
    await finalizeRes;

    await expect(page.getByText('DELIVERED', { exact: true }).first()).toBeVisible();

    // Verify completion warning and that recording is disabled
    await expect(page.getByText('Delivery Order Completed')).toBeVisible();
    await expect(page.locator('[data-testid="btn-submit-failed-attempt"]')).not.toBeVisible();
  });
});
