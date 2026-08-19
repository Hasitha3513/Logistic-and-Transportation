
import { test, expect } from '../../fixtures/authFixtures';
import { FuelIssuesPage } from '../../pages/FuelIssuesPage';

test.describe('@fuel Fuel Issuance & Policy Enforcement (US-05, US-31)', () => {
  test.beforeEach(async ({ fuelOperatorPage }) => {
    await fuelOperatorPage.route('**/api/fuel-issues*', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          { id: 'fi-1', voucherNumber: 'ISS-2026-001', issueDateTime: '2026-08-19T10:00:00Z', vehicle: { id: '32000000-0000-0000-0000-000000000001' }, station: { name: 'Central Depot Station' }, fuelType: 'DIESEL', quantity: 85, status: 'ISSUED', active: true },
        ],
        totalElements: 1,
      }),
    }));
  });

  test('E2E-FUEL-003: View fuel issuance list and status', async ({ fuelOperatorPage }) => {
    const page = new FuelIssuesPage(fuelOperatorPage);
    await page.goto();
    await expect(page.issuesTable).toContainText('ISS-2026-001');
  });
});
