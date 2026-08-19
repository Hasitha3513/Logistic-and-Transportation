
import { test, expect } from '../../fixtures/authFixtures';
import { FuelPurchasesPage } from '../../pages/FuelPurchasesPage';

test.describe('@fuel Fuel Purchase Orders & Bunker Receiving (US-32)', () => {
  test.beforeEach(async ({ fuelOperatorPage }) => {
    await fuelOperatorPage.route('**/api/fuel-purchases*', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'fp-1',
            purchaseNumber: 'PO-2026-001',
            purchaseDate: '2026-08-19',
            vendor: { id: '80000000-0000-0000-0000-000000000001', name: 'Lanka IOC', code: 'VND-IOC' },
            invoiceNumber: 'INV-2026-001',
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
  });

  test('E2E-FUEL-002: View fuel purchases list and statuses', async ({ fuelOperatorPage }) => {
    const page = new FuelPurchasesPage(fuelOperatorPage);
    await page.goto();
    await expect(page.purchasesTable).toContainText('PO-2026-001');
  });
});
