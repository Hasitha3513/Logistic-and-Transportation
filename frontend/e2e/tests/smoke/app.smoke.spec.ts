
import { test, expect } from '../../fixtures/authFixtures';
import { LoginPage } from '../../pages/LoginPage';
import { DashboardPage } from '../../pages/DashboardPage';
import { FleetVehiclesPage } from '../../pages/FleetVehiclesPage';
import { DriversPage } from '../../pages/DriversPage';
import { TripsPage } from '../../pages/TripsPage';

test.describe('@smoke Core Application Smoke Suite', () => {
  test('E2E-AUTH-001: Unauthenticated user can view login page and sign in', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await expect(loginPage.submitButton).toBeVisible();
  });

  test('E2E-SMK-001: Admin can access dashboard and main operations views', async ({ adminPage }) => {
    const dashboard = new DashboardPage(adminPage);
    await dashboard.goto();
    await dashboard.expectDashboardLoaded();

    const fleet = new FleetVehiclesPage(adminPage);
    await fleet.goto();
    await expect(fleet.createButton).toBeVisible();

    const drivers = new DriversPage(adminPage);
    await drivers.goto();
    await expect(drivers.createButton).toBeVisible();

    const trips = new TripsPage(adminPage);
    await trips.goto();
    await expect(trips.tripTable).toBeVisible();
  });
});
