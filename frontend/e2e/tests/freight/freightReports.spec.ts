import { test, expect } from '../../fixtures/authFixtures';

test.describe('US-29 freight reports', () => {
  test('renders tenant-scoped source metrics and honest incomplete utilization', async ({ adminPage: page }) => {
    await page.route('**/api/reports/freight/summary**', (route) => route.fulfill({ status: 200,
      contentType: 'application/json', body: JSON.stringify({ freightOrders: 2, manifests: 1, manifestItems: 3,
        loadPlans: 1, loadPlansByStatus: { DRAFT: 1 }, complianceOutcomes: { INCOMPLETE: 1 }, policies: 1,
        policiesByStatus: { ACTIVE: 1 }, claims: 1, claimsByStatus: { SUBMITTED: 1 }, settlements: 0,
        cargoExceptions: 1, exceptionsByStatus: { OPEN: 1 }, exceptionsByType: { DAMAGE: 1 }, unresolvedExceptions: 1 }) }));
    await page.route('**/api/reports/freight/shipments**', (route) => route.fulfill({ status: 200,
      contentType: 'application/json', body: JSON.stringify({ content: [{ freightOrderId: 'order-a',
        orderNumber: 'FO-TENANT-A', customerId: 'customer-a', manifestNumber: 'CM-A', loadPlanNumber: 'LP-A',
        cargoWeightKg: 125, complianceOutcome: 'INCOMPLETE', incompleteDiagnostics: ['VEHICLE_VOLUME_CAPACITY_UNAVAILABLE'],
        createdAt: '2026-08-01T00:00:00Z' }], page: 0, size: 20, totalElements: 1, totalPages: 1 }) }));

    await page.goto('/freight/reports');
    await expect(page.getByText('FO-TENANT-A')).toBeVisible();
    await expect(page.getByText('CM-A')).toBeVisible();
    await expect(page.getByText('INCOMPLETE').last()).toBeVisible();
    await expect(page.getByRole('button', { name: /export csv/i })).toBeVisible();
  });
});
