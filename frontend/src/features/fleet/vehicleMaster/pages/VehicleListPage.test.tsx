import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, delay, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, vi } from 'vitest';
import { appTheme } from '../../../../app/theme/theme';
import { server } from '../../../../test/server';
import type { Vehicle } from '../types/vehicle';
import VehicleListPage from './VehicleListPage';

let permissions = ['VEHICLE_VIEW'];

vi.mock('../../../../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: (permission: string) => permissions.includes(permission),
  }),
}));

vi.mock('../../../offlineSync/OfflineSyncProvider', () => ({
  useOptionalOfflineSync: () => undefined,
}));

const vehicles: Vehicle[] = [
  {
    id: 'vehicle-1', registrationNumber: 'WP-CAB-1201', chassisNumber: 'CH-1', engineNumber: 'EN-1',
    categoryId: 'category-1', typeId: 'type-1', manufacturer: 'Isuzu', model: 'NPR', manufactureYear: 2023,
    ownershipType: 'COMPANY_OWNED', operationalStatus: 'AVAILABLE', currentOdometerKm: 1200,
    engineHours: 80, capacityKg: 5500, active: true,
  },
  {
    id: 'vehicle-2', registrationNumber: 'WP-RENT-2', categoryId: 'category-1', typeId: 'type-1',
    manufacturer: 'Toyota', model: 'Dyna', ownershipType: 'LEASED', operationalStatus: 'ALLOCATED', active: true,
  },
  {
    id: 'vehicle-3', registrationNumber: 'WP-MNT-3', categoryId: 'category-1', typeId: 'type-1',
    ownershipType: 'COMPANY_OWNED', operationalStatus: 'MAINTENANCE', active: true,
  },
  {
    id: 'vehicle-4', registrationNumber: 'WP-OOS-4', categoryId: 'category-1', typeId: 'type-1',
    ownershipType: 'COMPANY_OWNED', operationalStatus: 'OUT_OF_SERVICE', active: false,
  },
  {
    id: 'vehicle-5', registrationNumber: 'WP-BRK-5', categoryId: 'category-1', typeId: 'type-1',
    ownershipType: 'COMPANY_OWNED', operationalStatus: 'BROKEN_DOWN', active: true,
  },
];

function renderPage(route = '/fleet/vehicles') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <ConfigProvider theme={appTheme}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={[route]}><VehicleListPage /></MemoryRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>,
  );
}

describe('VehicleListPage', () => {
  beforeEach(() => {
    permissions = ['VEHICLE_VIEW'];
    server.use(
      http.get('*/vehicles', () => HttpResponse.json(vehicles)),
      http.get('*/vehicle-categories', () => HttpResponse.json([{ id: 'category-1', name: 'Trucks', active: true }])),
      http.get('*/vehicle-types', () => HttpResponse.json([{ id: 'type-1', categoryId: 'category-1', name: 'Box Truck', active: true }])),
    );
  });

  it('renders actual status and lifecycle values and filters company/rental vehicles from one registry', async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText('WP-CAB-1201')).toBeInTheDocument();
    expect(screen.getByText('Available')).toBeInTheDocument();
    expect(screen.getByText('Allocated')).toBeInTheDocument();
    expect(screen.getByText('Maintenance')).toBeInTheDocument();
    expect(screen.getByText('Out of service')).toBeInTheDocument();
    expect(screen.getByText('Broken down')).toBeInTheDocument();

    await user.click(screen.getByRole('combobox', { name: 'Filter by ownership' }));
    await user.click(await screen.findByText('Leased / rental', { selector: '.ant-select-item-option-content' }));

    await waitFor(() => expect(screen.queryByText('WP-CAB-1201')).not.toBeInTheDocument());
    expect(screen.getByText('WP-RENT-2')).toBeInTheDocument();
  });

  it('supports vehicle search without a second backend request', async () => {
    let requests = 0;
    server.use(http.get('*/vehicles', () => { requests += 1; return HttpResponse.json(vehicles); }));
    renderPage();
    await screen.findByText('WP-CAB-1201');

    const search = screen.getByLabelText('Search vehicles');
    fireEvent.change(search, { target: { value: 'Toyota' } });
    fireEvent.keyDown(search, { key: 'Enter', code: 'Enter' });

    await waitFor(() => expect(screen.queryByText('WP-CAB-1201')).not.toBeInTheDocument());
    expect(screen.getByText('WP-RENT-2')).toBeInTheDocument();
    expect(requests).toBe(1);
  });

  it('keeps mutation controls hidden for a VEHICLE_VIEW-only actor', async () => {
    renderPage();
    await screen.findByText('WP-CAB-1201');

    expect(screen.getByText('Read-only access')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Create' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Deactivate' })).not.toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /view details/i })).toHaveLength(vehicles.length);
  });

  it('renders a stable empty state', async () => {
    server.use(http.get('*/vehicles', () => HttpResponse.json([])));
    renderPage();
    expect(await screen.findByText('No vehicles found')).toBeInTheDocument();
  });

  it('renders loading and backend error states', async () => {
    server.use(http.get('*/vehicles', async () => { await delay(100); return HttpResponse.json({}, { status: 500 }); }));
    renderPage();
    expect(document.querySelector('.ant-spin')).toBeInTheDocument();
    expect(await screen.findByText('Vehicle registry could not be loaded')).toBeInTheDocument();
  });
});
