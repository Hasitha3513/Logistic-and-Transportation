
import { test, expect } from '../../fixtures/authFixtures';
import { FleetVehiclesPage } from '../../pages/FleetVehiclesPage';
import { DriversPage } from '../../pages/DriversPage';
import { TripsPage } from '../../pages/TripsPage';

test.describe('@critical End-to-End Fleet to Trip Lifecycle Journey', () => {
  test('E2E-JOURNEY-001: Register vehicle & driver and navigate to operations', async ({ adminPage }) => {
    let vehiclesList: any[] = [];
    let driversList: any[] = [];

    await adminPage.route('**/api/vehicle-categories*', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: 'cat-1', name: 'Trucks' }]) }));
    await adminPage.route('**/api/vehicle-types*', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: 'type-1', name: 'Box Truck' }]) }));

    await adminPage.route('**/api/vehicles*', async (r) => {
      if (r.request().method() === 'POST') {
        const body = r.request().postDataJSON();
        const created = { id: '32000000-0000-0000-0000-000000000099', ...body, active: true };
        vehiclesList.push(created);
        return r.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(created) });
      }
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(vehiclesList) });
    });

    await adminPage.route('**/api/drivers*', async (r) => {
      if (r.request().method() === 'POST') {
        const body = r.request().postDataJSON();
        const created = { id: '40000000-0000-0000-0000-000000000099', ...body, status: 'AVAILABLE', active: true };
        driversList.push(created);
        return r.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(created) });
      }
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(driversList) });
    });

    await adminPage.route('**/api/trips*', (r) => r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          { id: '60000000-0000-0000-0000-000000000001', tripNumber: 'TRIP-2026-001', customerId: '10000000-0000-0000-0000-000000000001', priority: 'NORMAL', status: 'DRAFT', requestedStartTime: '2026-08-20T08:00:00Z', requestedEndTime: '2026-08-20T18:00:00Z', active: true },
        ],
        totalElements: 1,
      }),
    }));

    const fleetPage = new FleetVehiclesPage(adminPage);
    await fleetPage.goto();
    await fleetPage.createVehicle({ registrationNumber: 'WP-EXP-1122', manufacturer: 'Isuzu', model: 'Giga' });
    await expect(fleetPage.vehicleTable).toContainText('Isuzu');

    const driversPage = new DriversPage(adminPage);
    await driversPage.goto();
    await driversPage.createDriver({ employeeNumber: 'DRV-EXP-01', firstName: 'Kamal', lastName: 'Silva' });
    await expect(driversPage.driverTable).toContainText('Kamal Silva');

    const tripsPage = new TripsPage(adminPage);
    await tripsPage.goto();
    await expect(tripsPage.tripTable.first()).toContainText('TRIP-2026-001', { timeout: 15000 });
  });
});
