import { expect, test } from '@playwright/test';
import { adminLogin, authenticatePage } from '../../helpers/notificationTestApi';

test.describe('US-56 delivery order management', () => {
  test('E2E-MVP13-DEL-001: create, view, edit and validate a Delivery Order', async ({ page, request }) => {
    const admin = await adminLogin(request); await authenticatePage(page, admin);
    page.on('response', async (response) => {
      if (response.url().includes('/deliveries') && response.status() >= 400) {
        console.error('DELIVERY API ERROR:', response.status(), await response.text());
      }
    });
    const customersPromise = page.waitForResponse((res) => res.url().includes('/customers') && res.status() === 200);
    const locationsPromise = page.waitForResponse((res) => res.url().includes('/locations') && res.status() === 200);
    await page.goto('/deliveries/new');
    await Promise.all([customersPromise, locationsPromise]);
    const select = async (label: string, option: string) => {
      await page.getByLabel(label).first().locator('.ant-select-selector').click();
      const combobox = page.getByRole('combobox', { name: label });
      await combobox.fill(option);
      await combobox.press('ArrowDown');
      await combobox.press('Enter');
    };
    await select('Customer', 'Acme Distribution');
    await select('Origin', 'Colombo');
    await select('Destination', 'Kandy');
    await page.getByLabel('Window start').fill('2027-02-01 08:00:00');
    await page.getByLabel('Window end').fill('2027-02-01 12:00:00');
    await page.getByLabel('Instructions').fill('Call receiving desk');
    await page.getByRole('button', { name: 'Save delivery order' }).click();
    await expect(page).toHaveURL(/\/deliveries\/[0-9a-f-]+$/);
    const heading = page.getByRole('heading', { name: /DEL-\d{4}-\d{6}/ });
    await expect(heading).toBeVisible();
    const deliveryNumber = (await heading.textContent())?.trim() ?? '';

    await page.getByRole('button', { name: 'Validate readiness' }).click();
    await expect(page.getByText('READY FOR ASSIGNMENT').first()).toBeVisible();

    await page.getByRole('button', { name: 'Edit' }).click();
    await expect(page).toHaveURL(/\/deliveries\/[0-9a-f-]+\/edit$/);
    await page.getByLabel('Instructions').fill('Call receiving desk - updated instructions');
    await page.getByRole('button', { name: 'Save delivery order' }).click();
    await expect(page).toHaveURL(/\/deliveries\/[0-9a-f-]+$/);
    await expect(page.getByText('DRAFT', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('Call receiving desk - updated instructions')).toBeVisible();

    await page.getByRole('button', { name: 'Validate readiness' }).click();
    await expect(page.getByText('READY FOR ASSIGNMENT').first()).toBeVisible();

    await page.goto('/deliveries');
    await expect(page.getByText(deliveryNumber).first()).toBeVisible();
  });

  test('E2E-MVP13-DEL-002: rejects an invalid delivery window', async ({ page, request }) => {
    const admin = await adminLogin(request); await authenticatePage(page, admin); await page.goto('/deliveries/new');
    await page.getByLabel('Window start').fill('2027-02-02 12:00:00');
    await page.getByLabel('Window start').blur();
    await page.getByLabel('Window end').fill('2027-02-02 08:00:00');
    await page.getByLabel('Window end').blur();
    await page.getByRole('button', { name: 'Save delivery order' }).click();
    await expect(page.getByText('Window end must not precede its start')).toBeVisible();
  });
});
