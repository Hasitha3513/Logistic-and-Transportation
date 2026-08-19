
import { test, expect } from '../../fixtures/authFixtures';
import { BunkerTanksPage } from '../../pages/BunkerTanksPage';

test.describe('@fuel Bunker Tank Storage & Dip Readings (US-36)', () => {
  test.beforeEach(async ({ fuelOperatorPage }) => {
    await fuelOperatorPage.route('**/api/bunker-tanks*', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: '72000000-0000-0000-0000-000000000001', tankCode: 'BNK-01', tankName: 'Main Diesel Tank', fuelType: 'DIESEL', capacityLiters: 10000, currentStockLiters: 7500, availableCapacityLiters: 2500, active: true },
      ]),
    }));
  });

  test('E2E-FUEL-001: View bunker tank registry and stock balances', async ({ fuelOperatorPage }) => {
    const page = new BunkerTanksPage(fuelOperatorPage);
    await page.goto();
    await expect(page.tankTable).toContainText('BNK-01');
  });
});
