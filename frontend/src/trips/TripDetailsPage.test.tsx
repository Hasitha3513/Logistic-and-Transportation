import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';
import { AuthProvider } from '../auth/AuthContext';
import { server } from '../test/server';
import { appTheme } from '../app/theme/theme';

const trip = {
  id: 'trip-1', tripNumber: 'TRIP-000123', customerId: 'customer-1', routeId: 'route-1',
  originLocationId: 'location-1', destinationLocationId: 'location-2',
  requestedStartTime: '2026-08-15T02:30:00Z', requestedEndTime: '2026-08-15T07:30:00Z',
  priority: 'HIGH', vehicleId: 'vehicle-1', driverId: 'driver-1', status: 'ASSIGNED',
  cargoDescription: 'Medical supplies', requiredCapacityKg: 2500, passengerCount: 1,
  customerInstructions: 'Temperature controlled', notes: 'Call on arrival',
};

let vehicleAssignment: unknown;
let driverAssignment: unknown;
let routeAssignment: unknown;

function renderDetails() {
  vehicleAssignment = undefined;
  driverAssignment = undefined;
  routeAssignment = undefined;
  server.use(
    http.get('*/auth/me', () => HttpResponse.json({
      id: 'user-1', username: 'trip.operator', email: 'trip@example.com', firstName: 'Trip', lastName: 'Operator',
      active: true, roles: ['OPERATIONS'], permissions: [
        'TRIP_VIEW', 'VEHICLE_VIEW', 'DRIVER_VIEW', 'ROUTE_VIEW',
        'TRIP_ASSIGN_VEHICLE', 'TRIP_ASSIGN_DRIVER', 'TRIP_ASSIGN_ROUTE', 'VEHICLE_AVAILABILITY_VIEW', 'DRIVER_AVAILABILITY_VIEW',
      ],
    })),
    http.get('*/trips/:tripId/status-history', () => HttpResponse.json([
      {
        id: 'history-1', tripId: 'trip-1', fromStatus: 'APPROVED', toStatus: 'ASSIGNED',
        action: 'ASSIGN_DRIVER', driverId: 'driver-1', actor: 'trip.operator',
        details: 'Driver assigned', occurredAt: '2026-08-14T08:00:00Z',
      },
    ])),
    http.get('*/trips/:tripId', () => HttpResponse.json(trip)),
    http.get('*/customers', () => HttpResponse.json([{ id: 'customer-1', code: 'CUS-1', name: 'Central Hospital' }])),
    http.get('*/locations', () => HttpResponse.json([
      { id: 'location-1', code: 'CMB', name: 'Colombo' },
      { id: 'location-2', code: 'KDY', name: 'Kandy' },
      { id: 'location-3', code: 'KGL', name: 'Kegalle' },
    ])),
    http.get('*/vehicles', () => HttpResponse.json([
      { id: 'vehicle-1', registrationNumber: 'WP-CAB-1234', manufacturer: 'Isuzu', model: 'NPR', operationalStatus: 'AVAILABLE', capacityKg: 3500 },
      { id: 'vehicle-2', registrationNumber: 'WP-CAB-9999', manufacturer: 'Tata', model: 'Ultra', operationalStatus: 'MAINTENANCE', capacityKg: 5000 },
    ])),
    http.get('*/drivers', () => HttpResponse.json([
      { id: 'driver-1', employeeNumber: 'DRV-1', firstName: 'Nimal', lastName: 'Perera', status: 'AVAILABLE' },
      { id: 'driver-2', employeeNumber: 'DRV-2', firstName: 'Kamal', lastName: 'Silva', status: 'AVAILABLE' },
    ])),
    http.get('*/vehicles/:vehicleId/availability', ({ params }) => HttpResponse.json(params.vehicleId === 'vehicle-1'
      ? { available: true, reasons: [] }
      : { available: false, reasons: [{ code: 'MAINTENANCE_BLOCKED', message: 'Vehicle is blocked by maintenance status' }] })),
    http.get('*/drivers/:driverId/availability', ({ params }) => HttpResponse.json(params.driverId === 'driver-1'
      ? { available: true, reasons: [] }
      : { available: false, reasons: [{ code: 'LICENSE_EXPIRED', message: 'No applicable active license remains valid through the requested period' }] })),
    http.post('*/trips/:tripId/assign-vehicle', async ({ request }) => {
      vehicleAssignment = await request.json();
      return HttpResponse.json(trip);
    }),
    http.post('*/trips/:tripId/assign-driver', async ({ request }) => {
      driverAssignment = await request.json();
      return HttpResponse.json(trip);
    }),
    http.get('*/routes/:routeId', () => HttpResponse.json({
      id: 'route-1', code: 'CMB-KDY', name: 'Colombo to Kandy', originLocationId: 'location-1',
      destinationLocationId: 'location-2', plannedDistanceKm: 115, estimatedDurationMinutes: 210,
      active: true, stopLocationIds: ['location-3'],
    })),
    http.get('*/routes', () => HttpResponse.json([
      { id: 'route-1', code: 'CMB-KDY', name: 'Colombo to Kandy', originLocationId: 'location-1', destinationLocationId: 'location-2', active: true },
      { id: 'route-2', code: 'CMB-KDY-ALT', name: 'Colombo to Kandy Alternate', originLocationId: 'location-1', destinationLocationId: 'location-2', active: true },
    ])),
    http.post('*/trips/:tripId/assign-route', async ({ request }) => {
      routeAssignment = await request.json();
      return HttpResponse.json({ ...trip, routeId: 'route-2' });
    }),
  );

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <ConfigProvider theme={appTheme}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={['/trips/trip-1']}>
            <AuthProvider><App /></AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>,
  );
}

describe('TripDetailsPage', () => {
  it('renders core trip information and operational summaries', async () => {
    renderDetails();

    expect((await screen.findAllByText('TRIP-000123')).length).toBeGreaterThan(0);
    expect(screen.getByText('Central Hospital')).toBeInTheDocument();
    expect(screen.getAllByText('Colombo').length).toBeGreaterThan(0);
    expect(screen.getByText('Medical supplies')).toBeInTheDocument();
    expect(screen.getAllByText('Assigned').length).toBeGreaterThan(0);
  });

  it('shows backend route and history data in their sections', async () => {
    const user = userEvent.setup();
    renderDetails();
    await screen.findAllByText('TRIP-000123');

    await user.click(screen.getByRole('tab', { name: 'Route' }));
    expect((await screen.findAllByText('Colombo to Kandy')).length).toBeGreaterThan(0);
    expect(screen.getByText('Kegalle')).toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: 'History' }));
    expect(await screen.findByText('ASSIGN DRIVER')).toBeInTheDocument();
    expect(screen.getByText(/trip\.operator/)).toBeInTheDocument();
  });

  it('assigns a route through the authorized operational command', async () => {
    const user = userEvent.setup();
    renderDetails();
    await screen.findAllByText('TRIP-000123');

    await user.click(screen.getByRole('tab', { name: 'Route' }));
    await user.click(screen.getByRole('button', { name: /Change route/ }));
    await user.click(await screen.findByRole('combobox'));
    await user.click(await screen.findByText('CMB-KDY-ALT — Colombo to Kandy Alternate'));
    await user.click(screen.getByRole('button', { name: 'Assign' }));

    await waitFor(() => expect(routeAssignment).toEqual({ routeId: 'route-2' }));
  });

  it('renders backend vehicle rejection reasons and confirms an eligible vehicle', async () => {
    const user = userEvent.setup();
    renderDetails();
    await screen.findAllByText('TRIP-000123');

    await user.click(screen.getByRole('tab', { name: 'Assignments' }));
    await user.click(screen.getByRole('button', { name: 'Change vehicle' }));
    expect(await screen.findByText('Vehicle is blocked by maintenance status')).toBeInTheDocument();
    await user.click(screen.getByRole('radio', { name: 'Select WP-CAB-1234' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await user.click(screen.getByRole('button', { name: 'Confirm assignment' }));

    await waitFor(() => expect(vehicleAssignment).toEqual({ vehicleId: 'vehicle-1' }));
  });

  it('uses the requested licence class and backend reasons for driver assignment', async () => {
    const user = userEvent.setup();
    renderDetails();
    await screen.findAllByText('TRIP-000123');

    await user.click(screen.getByRole('tab', { name: 'Assignments' }));
    await user.click(screen.getByRole('button', { name: 'Change driver' }));
    await user.type(screen.getByPlaceholderText('Required licence class'), 'C');
    expect(await screen.findByText('No applicable active license remains valid through the requested period')).toBeInTheDocument();
    await user.click(screen.getByRole('radio', { name: 'Select Nimal Perera' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await user.click(screen.getByRole('button', { name: 'Confirm assignment' }));

    await waitFor(() => expect(driverAssignment).toEqual({ driverId: 'driver-1', requiredLicenseClass: 'C' }));
  });

  it('renders en-route operational events in the Trip Logs tab', async () => {
    const user = userEvent.setup();
    renderDetails();
    await screen.findAllByText('TRIP-000123');

    await user.click(screen.getByRole('tab', { name: 'Trip Logs' }));
    expect(await screen.findByText('En-Route Checkpoints & Operational Events')).toBeInTheDocument();
  });

  it('explains future backend-supported sections in a modal', async () => {
    const user = userEvent.setup();
    renderDetails();
    await screen.findAllByText('TRIP-000123');

    await user.click(screen.getByRole('tab', { name: /Exceptions/ }));
    await user.click(screen.getByRole('button', { name: 'About this section' }));
    expect(await screen.findByText('Future backend-supported section')).toBeInTheDocument();
  });
});
