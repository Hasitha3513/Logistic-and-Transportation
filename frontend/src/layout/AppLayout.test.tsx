import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';
import { AuthProvider } from '../auth/AuthContext';
import type { CurrentUser } from '../auth/types';
import { appTheme } from '../app/theme/theme';
import { server } from '../test/server';
import { vi } from 'vitest';

vi.mock('../features/offlineSync/OfflineSyncProvider', () => ({
  useOptionalOfflineSync: () => undefined,
  useOfflineSync: () => ({
    enqueueOperation: vi.fn(),
    syncNow: vi.fn(() => Promise.resolve()),
    registerPostApply: vi.fn(() => () => undefined),
    getOperationsForAggregate: vi.fn(() => Promise.resolve([])),
    operationsRevision: 0,
  }),
}));

const operator: CurrentUser = {
  id: 'd5880745-1a9f-4f7f-bdc8-ff3e257962f1',
  username: 'alex.operator',
  email: 'alex@example.com',
  firstName: 'Alex',
  lastName: 'Morgan',
  active: true,
  roles: ['OPERATIONS'],
  permissions: ['DASHBOARD_VIEW', 'VEHICLE_VIEW', 'DRIVER_VIEW', 'TRIP_VIEW'],
};

const administrator: CurrentUser = {
  ...operator,
  firstName: 'Local',
  lastName: 'Administrator',
  roles: ['LOCAL_MVP_ADMIN'],
  permissions: [
    'DASHBOARD_VIEW', 'VEHICLE_VIEW', 'VEHICLE_CREATE', 'VEHICLE_UPDATE',
    'VEHICLE_STATUS_UPDATE', 'VEHICLE_DOCUMENT_MANAGE',
  ],
};

function renderApp(user: CurrentUser = operator, route = '/') {
  server.use(
    http.get('*/auth/me', () => HttpResponse.json(user)),
    http.get('*/dashboard/operations', () => HttpResponse.json({ date: '2026-08-15', status: 'READY' })),
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <ConfigProvider theme={appTheme}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={[route]}>
            <AuthProvider><App /></AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>,
  );
}

describe('AppLayout', () => {
  it('renders the enterprise shell and current user', async () => {
    renderApp();

    expect(await screen.findByText('TransportOps')).toBeInTheDocument();
    expect(screen.getByText('Alex Morgan')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Dashboard', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /collapse navigation/i })).toBeInTheDocument();
  });

  it('shows only navigation authorized by backend permissions', async () => {
    const user = userEvent.setup();
    renderApp();

    await screen.findByText('TransportOps');
    await user.click(screen.getByText('Fleet Management'));
    expect(screen.getByText('Vehicle Master')).toBeInTheDocument();
    expect(screen.getByText('Fleet Categories')).toBeInTheDocument();
    expect(screen.getAllByText('Drivers').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Trips').length).toBeGreaterThan(0);
    expect(screen.queryByText('Routes')).not.toBeInTheDocument();
    expect(screen.queryByText('Administration')).not.toBeInTheDocument();
  });

  it('renders route title and breadcrumbs for permitted module pages', async () => {
    server.use(
      http.get('*/vehicles', () => HttpResponse.json([])),
      http.get('*/vehicle-categories', () => HttpResponse.json([])),
      http.get('*/vehicle-types', () => HttpResponse.json([])),
    );
    renderApp(operator, '/fleet/vehicles');

    expect(await screen.findByRole('heading', { name: 'Vehicle Master', level: 2 })).toBeInTheDocument();
    expect(screen.getAllByRole('heading', { level: 2 })).toHaveLength(1);
    expect(screen.getByText('Live vehicle master data from the fleet module.')).toBeInTheDocument();
    await waitFor(() => expect(screen.getAllByText('Fleet Management').length).toBeGreaterThan(0));
  });

  it('shows the signed-in actor roles and business permissions', async () => {
    const user = userEvent.setup();
    renderApp(administrator);

    await user.click(await screen.findByRole('button', { name: /Local Administrator/i }));
    await user.click(await screen.findByText('Access & permissions'));

    expect(await screen.findByText('LOCAL MVP ADMIN')).toBeInTheDocument();
    expect(screen.getByText('VEHICLE DOCUMENT MANAGE')).toBeInTheDocument();
    expect(screen.getByText('Backend authorization remains authoritative')).toBeInTheDocument();
  });

  it('opens complete vehicle and compliance details for an authorized administrator', async () => {
    const user = userEvent.setup();
    const vehicle = {
      id: '32000000-0000-0000-0000-000000000001', registrationNumber: 'WP-CAB-1201',
      manufacturer: 'Isuzu', model: 'NPR', capacityKg: 5500, operationalStatus: 'AVAILABLE', active: true,
    };
    server.use(
      http.get('*/vehicles', () => HttpResponse.json([vehicle])),
      http.get('*/vehicle-categories', () => HttpResponse.json([])),
      http.get('*/vehicle-types', () => HttpResponse.json([])),
      http.get(`*/vehicles/${vehicle.id}`, () => HttpResponse.json(vehicle)),
      http.get(`*/vehicles/${vehicle.id}/documents`, () => HttpResponse.json([{ id: 'doc-1', documentNumber: 'INS-WP-1201', status: 'ACTIVE' }])),
      http.get(`*/vehicles/${vehicle.id}/readings`, () => HttpResponse.json({ content: [], page: 0, limit: 50, totalElements: 0, totalPages: 0 })),
      http.get(`*/vehicles/${vehicle.id}/readings/latest`, () => HttpResponse.json({ vehicleId: vehicle.id, odometer: null, engineHours: null })),
      http.get(`*/vehicles/${vehicle.id}/meter-resets`, () => HttpResponse.json([])),
      http.get(`*/vehicles/${vehicle.id}/maintenance-schedules`, () => HttpResponse.json([])),
      http.get(`*/vehicles/${vehicle.id}/lubricant-logs`, () => HttpResponse.json([])),
      http.get(`*/vehicles/${vehicle.id}/mileage*`, () => HttpResponse.json({
        vehicleId: vehicle.id, from: '2026-07-17T00:00:00Z', to: '2026-08-16T23:59:59Z',
        openingOdometer: null, closingOdometer: null, distanceTravelledKm: 0,
        openingEngineHours: null, closingEngineHours: null, engineHoursUsed: 0,
        meterResetCount: 0, abnormalDetected: false,
        coverageStatus: 'NO_DATA',
      })),
    );
    renderApp(administrator, '/fleet/vehicles');

    expect(await screen.findByText('Full management access')).toBeInTheDocument();
    await user.click(await screen.findByRole('button', { name: /view details/i }));

    expect(await screen.findByText('Vehicle registry details')).toBeInTheDocument();
    expect(screen.getByText('INS-WP-1201')).toBeInTheDocument();
    expect(screen.getByText('Vehicle documents')).toBeInTheDocument();
  });
});
