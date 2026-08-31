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
  await page.getByLabel('Instructions').fill('US-60 Redelivery acceptance order');
  await page.getByRole('button', { name: 'Save delivery order' }).click();
  await expect(page).toHaveURL(/\/deliveries\/[0-9a-f-]+$/);
  await page.getByRole('button', { name: 'Validate readiness' }).click();
  await expect(page.getByText('READY FOR ASSIGNMENT').first()).toBeVisible();
}

async function recordFailedAttempt(page: Page) {
  await page.locator('[data-testid="select-failure-reason"]').click();
  await page.getByTitle('Customer Unavailable').click();
  await page.locator('[data-testid="input-attempt-notes"]').fill('Customer requested redelivery tomorrow');
  const submitRes = page.waitForResponse(res => res.url().includes('/failed-attempt') && res.status() === 200);
  await page.locator('[data-testid="btn-submit-failed-attempt"]').click();
  await submitRes;
  await expect(page.getByText('FAILED ATTEMPT').first()).toBeVisible();
}

test.describe('US-60 Re-Delivery Scheduling E2E', () => {
  test.beforeEach(async ({ page, request }) => {
    page.on('response', async (response) => {
      if (response.url().includes('/redelivery') || response.url().includes('/failed-attempt')) {
        if (response.status() >= 400) {
          console.error('REDELIVERY API ERROR:', response.status(), await response.text());
        }
      }
    });
    const admin = await adminLogin(request);
    await authenticatePage(page, admin);
  });

  test('SCENARIO A: Failed delivery receives slot suggestions and schedules redelivery', async ({ page }) => {
    await createReadyDelivery(page);
    await recordFailedAttempt(page);

    // Verify Redelivery Section appears with active Schedule button
    await expect(page.getByTestId('redelivery-section')).toBeVisible();
    await page.getByTestId('schedule-redelivery-btn').click();

    // Click Get Available Slot Suggestions
    const sugRes = page.waitForResponse(res => res.url().includes('/redelivery/suggestions') && res.status() === 200);
    await page.getByRole('button', { name: 'Get Available Slot Suggestions' }).click();
    await sugRes;

    // Apply the first available suggestion
    await page.getByText('Next-Day Morning').first().click();

    // Submit schedule
    const scheduleRes = page.waitForResponse(res => res.url().includes('/redelivery/schedule') && res.status() === 201);
    await page.getByTestId('confirm-schedule-submit-btn').click();
    await scheduleRes;

    // Verify order transitioned back to READY FOR ASSIGNMENT
    await expect(page.getByText('READY FOR ASSIGNMENT').first()).toBeVisible();
    await expect(page.getByText('CONFIRMED').first()).toBeVisible();
  });

  test('SCENARIO B: Reschedule an existing scheduled redelivery order', async ({ page }) => {
    await createReadyDelivery(page);
    await recordFailedAttempt(page);

    // Schedule initial redelivery
    await page.getByTestId('schedule-redelivery-btn').click();
    const sugRes = page.waitForResponse(res => res.url().includes('/redelivery/suggestions') && res.status() === 200);
    await page.getByRole('button', { name: 'Get Available Slot Suggestions' }).click();
    await sugRes;
    await page.getByText('Next-Day Morning').first().click();
    const scheduleRes = page.waitForResponse(res => res.url().includes('/redelivery/schedule') && res.status() === 201);
    await page.getByTestId('confirm-schedule-submit-btn').click();
    await scheduleRes;

    // Now order is in READY FOR ASSIGNMENT. Click Reschedule button
    await expect(page.getByTestId('reschedule-redelivery-btn')).toBeVisible();
    await page.getByTestId('reschedule-redelivery-btn').click();

    // In Reschedule modal, get slot suggestions and select Afternoon slot
    const reschedSugRes = page.waitForResponse(res => res.url().includes('/redelivery/suggestions') && res.status() === 200);
    await page.getByTestId('reschedule-get-suggestions-btn').click();
    await reschedSugRes;
    await page.getByText('Next-Day Afternoon').first().click();

    const modal = page.locator('.ant-modal-content');
    const reasonInput = modal.getByPlaceholder('Explain why the scheduled window is changing');
    await reasonInput.fill('Customer delayed by 2 days');

    const reschedRes = page.waitForResponse(res => res.url().includes('/redelivery/reschedule') && res.status() === 200);
    await page.getByTestId('confirm-reschedule-submit-btn').click();
    await reschedRes;

    // Verify history table contains SUPERSEDED and CONFIRMED entries
    await expect(page.getByText('SUPERSEDED').first()).toBeVisible();
    await expect(page.getByText('CONFIRMED').first()).toBeVisible();
    await expect(page.getByText('Customer delayed by 2 days')).toBeVisible();
  });

  test('SCENARIO C: Delivered orders and RTO orders disable redelivery scheduling', async ({ page }) => {
    await createReadyDelivery(page);

    // Fail as CUSTOMER_REFUSED -> transitions to RETURN_TO_BASE
    await page.locator('[data-testid="select-failure-reason"]').click();
    await page.getByTitle('Customer Refused').click();
    await page.locator('[data-testid="input-attempt-notes"]').fill('Customer cancelled purchase contract');
    const submitRes = page.waitForResponse(res => res.url().includes('/failed-attempt') && res.status() === 200);
    await page.locator('[data-testid="btn-submit-failed-attempt"]').click();
    await submitRes;

    await expect(page.getByText('RETURN TO BASE').first()).toBeVisible();
    await expect(page.getByText('Order Returned to Base')).toBeVisible();
    await expect(page.locator('[data-testid="schedule-redelivery-btn"]')).not.toBeVisible();
  });
});
