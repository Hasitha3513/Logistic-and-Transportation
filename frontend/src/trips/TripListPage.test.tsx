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
import { tripStatusPresentation } from '../components/status/StatusTags';

const trips = [
  {
    id: 'trip-1',
    tripNumber: 'TRIP-000123',
    customerId: 'customer-1',
    originLocationId: 'location-1',
    destinationLocationId: 'location-2',
    requestedStartTime: '2026-08-15T02:30:00Z',
    requestedEndTime: '2026-08-15T07:30:00Z',
    priority: 'HIGH',
    vehicleId: 'vehicle-1',
    driverId: 'driver-1',
    status: 'ASSIGNED',
    cargoDescription: 'Medical supplies',
  },
  {
    id: 'trip-2',
    tripNumber: 'TRIP-000124',
    customerId: 'customer-2',
    originLocationId: 'location-2',
    destinationLocationId: 'location-1',
    requestedStartTime: '2026-08-16T02:30:00Z',
    requestedEndTime: '2026-08-16T06:30:00Z',
    priority: 'NORMAL',
    status: 'DRAFT',
    cargoDescription: 'General cargo',
  },
];

function renderTrips() {
  server.use(
    http.get('*/auth/me', () => HttpResponse.json({
      id: 'user-1', username: 'trip.operator', email: 'trip@example.com',
      firstName: 'Trip', lastName: 'Operator', active: true, roles: ['OPERATIONS'],
      permissions: ['TRIP_VIEW', 'VEHICLE_VIEW', 'DRIVER_VIEW'],
    })),
    http.get('*/trips', () => HttpResponse.json(trips)),
    http.get('*/customers', () => HttpResponse.json([
      { id: 'customer-1', code: 'CUS-1', name: 'Central Hospital' },
      { id: 'customer-2', code: 'CUS-2', name: 'Harbour Stores' },
    ])),
    http.get('*/locations', () => HttpResponse.json([
      { id: 'location-1', code: 'CMB', name: 'Colombo' },
      { id: 'location-2', code: 'KDY', name: 'Kandy' },
    ])),
    http.get('*/vehicles', () => HttpResponse.json([
      { id: 'vehicle-1', registrationNumber: 'WP-CAB-1234' },
    ])),
    http.get('*/drivers', () => HttpResponse.json([
      { id: 'driver-1', employeeNumber: 'DRV-1', firstName: 'Nimal', lastName: 'Perera' },
    ])),
  );

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <ConfigProvider theme={appTheme}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={['/trips']}>
            <AuthProvider><App /></AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>,
  );
}

describe('TripListPage', () => {
  it('renders trip operations with resolved locations and centralized status tags', async () => {
    renderTrips();

    expect(await screen.findByText('TRIP-000123')).toBeInTheDocument();
    expect(screen.getAllByText('Colombo').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Kandy').length).toBeGreaterThan(0);
    expect(screen.getByText('Assigned')).toBeInTheDocument();
    expect(screen.getByText(/Client pagination fallback/)).toBeInTheDocument();
  });

  it('filters an unpaged backend response without fetching the entire dataset twice', async () => {
    const user = userEvent.setup();
    renderTrips();
    await screen.findByText('TRIP-000124');

    const search = screen.getByPlaceholderText('Search trip number, customer, or cargo');
    await user.type(search, 'TRIP-000123{Enter}');

    await waitFor(() => expect(screen.queryByText('TRIP-000124')).not.toBeInTheDocument());
    expect(screen.getByText('TRIP-000123')).toBeInTheDocument();
  });

  it('provides one status presentation source for known and future statuses', () => {
    expect(tripStatusPresentation('IN_PROGRESS')).toEqual({ color: 'gold', label: 'In progress' });
    expect(tripStatusPresentation('awaiting_review')).toEqual({ color: 'default', label: 'Awaiting review' });
  });
});
