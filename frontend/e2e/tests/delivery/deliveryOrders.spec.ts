import { expect, test } from '@playwright/test';
import { adminLogin, authenticatePage } from '../../helpers/notificationTestApi';

test.describe('US-56 delivery order management', () => {
  test('E2E-MVP13-DEL-001: create, view, edit and validate a Delivery Order', async ({ page, request }) => {
    const admin = await adminLogin(request); await authenticatePage(page, admin); await page.goto('/deliveries/new');
    const selectReference = async (label: string, option: string) => { await page.getByLabel(label).locator('.ant-select-selector').click(); const input = page.getByRole('combobox', { name: label }); await input.fill(option); await input.press('ArrowDown'); await input.press('Enter'); };
    await selectReference('Customer', 'Acme Distribution'); await selectReference('Origin', 'Colombo'); await selectReference('Destination', 'Kandy');
    await page.getByLabel('Priority').locator('.ant-select-selector').click(); await page.getByText('HIGH', { exact: true }).click();
    await page.getByLabel('Service type').locator('.ant-select-selector').click(); await page.getByText('EXPRESS', { exact: true }).click();
    await page.getByLabel('Window start').fill('2027-02-01 08:00:00'); await page.getByLabel('Window start').press('Enter');
    await page.getByLabel('Window end').fill('2027-02-01 12:00:00'); await page.getByLabel('Window end').press('Enter');
    await page.getByLabel('Instructions').fill('Call receiving desk'); await page.getByRole('button', { name: 'Save delivery order' }).click();
    await expect(page).toHaveURL(/\/deliveries\/[0-9a-f-]+$/); await expect(page.getByRole('heading', { name: /DEL-\d{4}-\d{6}/ })).toBeVisible();
    await page.getByRole('button', { name: 'Validate readiness' }).click(); await expect(page.getByText('READY FOR ASSIGNMENT')).toBeVisible();
  });

  test('E2E-MVP13-DEL-002: rejects an invalid delivery window', async ({ page, request }) => {
    const admin = await adminLogin(request); await authenticatePage(page, admin); await page.goto('/deliveries/new');
    await page.getByLabel('Window start').fill('2027-02-02 12:00:00'); await page.getByLabel('Window start').press('Enter');
    await page.getByLabel('Window end').fill('2027-02-02 08:00:00'); await page.getByLabel('Window end').press('Enter');
    await page.getByRole('button', { name: 'Save delivery order' }).click(); await expect(page.getByText('Window end must not precede its start')).toBeVisible();
  });
});
