import { test, expect } from '@playwright/test';

test.describe('Delivery Analytics UX', () => {
  test.beforeEach(async ({ page }) => {
    // Mock authentication and permissions
    await page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '11111111-1111-1111-1111-111111111111',
          username: 'admin.tester',
          roles: ['ADMIN'],
          permissions: ['DELIVERY_VIEW', 'DELIVERY_ANALYTICS_VIEW', 'DASHBOARD_VIEW'],
          tenantId: '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a',
        }),
      });
    });

    // Mock summary API endpoint
    await page.route('**/api/v1/deliveries/analytics/summary*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          period: { from: '2026-08-01', to: '2026-08-31' },
          totalOrders: 120,
          activeOrders: 15,
          terminalCompletedOrders: 105,
          deliveredOrders: 95,
          returnedToBaseOrders: 10,
          orderSuccessRate: 90.48,
          firstAttemptSuccessRate: 80.0,
          onTimeDeliveredOrders: 85,
          lateDeliveredOrders: 10,
          onTimeDeliveryRate: 89.47,
          lateDeliveryRate: 10.53,
          averageDelayMinutes: 24.5,
          totalFailedAttempts: 25,
          averageFailedAttemptsPerOrder: 0.21,
          redeliveredOrders: 12,
          redeliveryRate: 10.0,
          redeliverySuccessRate: 83.33,
          returnToBaseRate: 9.52,
        }),
      });
    });

    // Mock failures API endpoint
    await page.route('**/api/v1/deliveries/analytics/failures*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            failureReason: 'CUSTOMER_UNAVAILABLE',
            count: 15,
            percentage: 60.0,
            redeliveryEligibleCount: 15,
            returnToBaseCount: 0,
            escalatedCount: 0,
          },
          {
            failureReason: 'CUSTOMER_REFUSED',
            count: 10,
            percentage: 40.0,
            redeliveryEligibleCount: 0,
            returnToBaseCount: 10,
            escalatedCount: 0,
          },
        ]),
      });
    });

    // Mock regions API endpoint
    await page.route('**/api/v1/deliveries/analytics/regions*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            destinationLocationId: '20000000-0000-0000-0000-000000000001',
            locationCode: 'LOC-CMB',
            locationName: 'Colombo Central Hub',
            city: 'Colombo',
            totalOrders: 70,
            deliveredOrders: 65,
            returnedToBaseOrders: 5,
            orderSuccessRate: 92.86,
            onTimeDeliveredOrders: 60,
            onTimeDeliveryRate: 92.31,
            averageDelayMinutes: 18.0,
            failedAttemptCount: 10,
          },
        ]),
      });
    });

    // Mock trends API endpoint
    await page.route('**/api/v1/deliveries/analytics/trends*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            bucketDate: '2026-08-15',
            totalCreated: 20,
            delivered: 18,
            failedAttempts: 2,
            returnedToBase: 1,
            onTimeDelivered: 16,
            lateDelivered: 2,
          },
        ]),
      });
    });
  });

  test('renders delivery analytics dashboard with KPI metrics and tab navigation', async ({ page }) => {
    await page.goto('/deliveries/analytics');

    // Heading verification
    await expect(page.getByRole('heading', { name: 'Delivery Performance Analytics' })).toBeVisible();

    // KPI Cards verification
    await expect(page.getByText('Order Success Rate')).toBeVisible();
    await expect(page.getByText('90.5%')).toBeVisible();
    await expect(page.getByText('(95/105)')).toBeVisible();

    await expect(page.getByText('On-Time Delivery Rate')).toBeVisible();
    await expect(page.getByText('89.5%')).toBeVisible();
    await expect(page.getByText(/Avg Late Delay: 24.5 mins/)).toBeVisible();

    // Tabs switching
    await expect(page.getByText('Trends & Volumes')).toBeVisible();
    await expect(page.getByText('2026-08-15')).toBeVisible();

    // Switch to Regional Performance
    await page.getByRole('tab', { name: 'Regional Performance' }).click();
    await expect(page.getByText('Colombo Central Hub')).toBeVisible();
    await expect(page.getByText('LOC-CMB')).toBeVisible();

    // Switch to Failure Analysis
    await page.getByRole('tab', { name: 'Failure Analysis' }).click();
    await expect(page.getByText('CUSTOMER UNAVAILABLE')).toBeVisible();
    await expect(page.getByText('CUSTOMER REFUSED')).toBeVisible();
  });
});
