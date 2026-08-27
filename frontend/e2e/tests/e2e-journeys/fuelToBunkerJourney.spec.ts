
import { test, expect } from '../../fixtures/authFixtures';
import { BunkerTanksPage } from '../../pages/BunkerTanksPage';
import { FuelPurchasesPage } from '../../pages/FuelPurchasesPage';
import { FuelIssuesPage } from '../../pages/FuelIssuesPage';

test.describe('@critical End-to-End Fuel Purchase to Bunker Issue Journey', () => {
  test('E2E-JOURNEY-002: Track bunker inventory and purchase/issue workflows', async ({ adminPage }) => {
    await adminPage.route('**/api/bunker-tanks*', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: '72000000-0000-0000-0000-000000000001', tankCode: 'BNK-DEPOT-01', tankName: 'Depot Tank 1', fuelType: 'DIESEL', capacityLiters: 10000, currentStockLiters: 8000, availableCapacityLiters: 2000, active: true },
      ]),
    }));

    await adminPage.route('**/api/fuel-purchases*', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'fp-1',
            purchaseNumber: 'PO-2026-99',
            purchaseDate: '2026-08-19',
            vendor: { id: '80000000-0000-0000-0000-000000000001', name: 'Lanka IOC', code: 'VND-IOC' },
            invoiceNumber: 'INV-2026-99',
            currencyCode: 'LKR',
            fuelType: 'DIESEL',
            quantity: 2000,
            unitPrice: 310,
            taxAmount: 0,
            totalAmount: 620000,
            status: 'RECEIVED',
            reconciliationStatus: 'RECONCILED',
            active: true,
          },
        ],
        totalElements: 1,
      }),
    }));

    await adminPage.route('**/api/fuel-issues*', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          { id: 'fi-1', voucherNumber: 'ISS-2026-99', issueDateTime: '2026-08-19T10:00:00Z', vehicle: { id: '32000000-0000-0000-0000-000000000001' }, station: { name: 'Central Depot Station' }, fuelType: 'DIESEL', quantity: 75, status: 'ISSUED', active: true },
        ],
        totalElements: 1,
      }),
    }));

    const bunkerPage = new BunkerTanksPage(adminPage);
    await bunkerPage.goto();
    await expect(bunkerPage.tankTable).toContainText('BNK-DEPOT-01');

    const purchasePage = new FuelPurchasesPage(adminPage);
    await purchasePage.goto();
    await expect(purchasePage.purchasesTable).toContainText('PO-2026-99');

    const issuePage = new FuelIssuesPage(adminPage);
    await issuePage.goto();
    await expect(issuePage.issuesTable).toContainText('ISS-2026-99');
  });
});
