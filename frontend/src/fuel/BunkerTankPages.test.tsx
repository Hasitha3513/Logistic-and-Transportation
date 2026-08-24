import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';
import { appTheme } from '../app/theme/theme';
import { AuthProvider } from '../auth/AuthContext';
import { server } from '../test/server';
import type { BunkerStockMovement, BunkerTank, BunkerTankBalance, DipReading } from './bunkerTypes';

const mockStation = {
  id: 'station-1',
  code: 'STN-COL-01',
  name: 'Colombo Central Depot',
  stationType: 'INTERNAL' as const,
  active: true,
};

const mockStationExternal = {
  id: 'station-ext-1',
  code: 'STN-EXT-01',
  name: 'External Highway Station',
  stationType: 'EXTERNAL' as const,
  active: true,
};

const mockTank1: BunkerTank = {
  id: 'tank-1',
  fuelStationId: 'station-1',
  tankCode: 'BNK-DSL-01',
  tankName: 'Main Diesel Tank',
  fuelType: 'DIESEL',
  capacityLiters: 10000,
  currentStockLiters: 6500,
  availableCapacityLiters: 3500,
  minimumStockLiters: 1500,
  status: 'ACTIVE',
  lowStock: false,
  active: true,
  createdAt: '2026-08-16T10:00:00Z',
  updatedAt: '2026-08-16T10:00:00Z',
};

const mockTank2: BunkerTank = {
  id: 'tank-2',
  fuelStationId: 'station-1',
  tankCode: 'BNK-DSL-02',
  tankName: 'Auxiliary Diesel Tank',
  fuelType: 'DIESEL',
  capacityLiters: 8000,
  currentStockLiters: 1200,
  availableCapacityLiters: 6800,
  minimumStockLiters: 1500,
  status: 'ACTIVE',
  lowStock: true,
  active: true,
  createdAt: '2026-08-16T10:00:00Z',
  updatedAt: '2026-08-16T10:00:00Z',
};

const mockBalance: BunkerTankBalance = {
  tankId: 'tank-1',
  fuelStationId: 'station-1',
  tankCode: 'BNK-DSL-01',
  tankName: 'Main Diesel Tank',
  fuelType: 'DIESEL',
  capacityLiters: 10000,
  currentStockLiters: 6500,
  availableCapacityLiters: 3500,
  minimumStockLiters: 1500,
  status: 'ACTIVE',
  stockStatus: 'NORMAL',
  latestDipQuantityLiters: 6480,
  latestDipAt: '2026-08-17T08:00:00Z',
  latestVarianceLiters: -20,
};

const mockMovements: BunkerStockMovement[] = [
  {
    id: 'mov-1',
    tankId: 'tank-1',
    movementType: 'OPENING_BALANCE',
    quantityLiters: 5000,
    resultingBalanceLiters: 5000,
    referenceType: 'OPENING_BALANCE',
    occurredAt: '2026-08-16T10:00:00Z',
    createdBy: 'admin',
    reason: 'Initial commissioning balance',
    createdAt: '2026-08-16T10:00:00Z',
  },
  {
    id: 'mov-2',
    tankId: 'tank-1',
    movementType: 'PURCHASE_RECEIPT',
    quantityLiters: 2000,
    resultingBalanceLiters: 7000,
    referenceType: 'FUEL_PURCHASE',
    occurredAt: '2026-08-17T09:00:00Z',
    createdBy: 'fuel.manager',
    reason: 'Bulk delivery purchase FP-001',
    createdAt: '2026-08-17T09:00:00Z',
  },
  {
    id: 'mov-3',
    tankId: 'tank-1',
    movementType: 'FUEL_ISSUE',
    quantityLiters: 500,
    resultingBalanceLiters: 6500,
    referenceType: 'FUEL_ISSUE',
    occurredAt: '2026-08-17T11:00:00Z',
    createdBy: 'fuel.operator',
    reason: 'Vehicle refuel VCH-01',
    createdAt: '2026-08-17T11:00:00Z',
  },
];

const mockDips: DipReading[] = [
  {
    id: 'dip-1',
    tankId: 'tank-1',
    physicalQuantityLiters: 6480,
    bookQuantityAtMeasurement: 6500,
    varianceQuantityLiters: -20,
    measuredAt: '2026-08-17T08:00:00Z',
    measuredBy: 'dip.inspector',
    notes: 'Morning manual dip stick check',
    createdAt: '2026-08-17T08:00:00Z',
  },
];

function setupBunkerHandlers(
  permissions: string[],
  tanks: BunkerTank[] = [mockTank1, mockTank2],
  balance: BunkerTankBalance = mockBalance,
  movements: BunkerStockMovement[] = mockMovements,
  dips: DipReading[] = mockDips
) {
  server.use(
    http.get('*/auth/me', () =>
      HttpResponse.json({
        id: 'user-1',
        username: 'fuel.manager',
        firstName: 'Fuel',
        lastName: 'Manager',
        active: true,
        roles: ['FUEL_MANAGER'],
        permissions,
      })
    ),
    http.get('*/fuel-stations', () => HttpResponse.json([mockStation, mockStationExternal])),
    http.get('*/bunker-tanks', () => HttpResponse.json(tanks)),
    http.get('*/bunker-tanks/:id', ({ params }) => {
      const found = tanks.find((t) => t.id === params.id) || mockTank1;
      return HttpResponse.json(found);
    }),
    http.get('*/bunker-tanks/:id/balance', () => HttpResponse.json(balance)),
    http.get('*/bunker-tanks/:id/movements', () =>
      HttpResponse.json({
        items: movements,
        page: 0,
        limit: 15,
        totalElements: movements.length,
        totalPages: 1,
      })
    ),
    http.get('*/bunker-tanks/:id/dip-readings', () => HttpResponse.json(dips)),
    http.post('*/bunker-tanks', async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json({
        ...mockTank1,
        id: 'tank-new-1',
        tankCode: body.tankCode,
        tankName: body.tankName,
        capacityLiters: body.capacityLiters,
      });
    }),
    http.put('*/bunker-tanks/:id', async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json({ ...mockTank1, ...body });
    }),
    http.post('*/bunker-tanks/:id/dip-readings', async ({ request }) => {
      const body = (await request.json()) as { physicalQuantityLiters: number; notes?: string };
      return HttpResponse.json({
        id: 'dip-new',
        tankId: 'tank-1',
        physicalQuantityLiters: body.physicalQuantityLiters,
        bookQuantityAtMeasurement: 6500,
        varianceQuantityLiters: body.physicalQuantityLiters - 6500,
        measuredAt: new Date().toISOString(),
        notes: body.notes,
        createdAt: new Date().toISOString(),
      });
    }),
    http.post('*/bunker-tanks/:id/adjustments', async ({ request }) => {
      const body = (await request.json()) as { quantityDeltaLiters: number; reason: string };
      return HttpResponse.json({
        id: 'adj-new',
        tankId: 'tank-1',
        quantityDeltaLiters: body.quantityDeltaLiters,
        reason: body.reason,
        occurredAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      });
    }),
    http.post('*/bunker-transfers', async () => {
      return new HttpResponse(null, { status: 200 });
    })
  );
}

function renderAt(path: string) {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return render(
    <ConfigProvider theme={appTheme}>
      <AntApp>
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={[path]}>
            <AuthProvider>
              <App />
            </AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  );
}

describe('Bunker Management Frontend UI', () => {
  it('renders bunker tank list with stock status and balance information', async () => {
    setupBunkerHandlers([
      'BUNKER_VIEW',
      'BUNKER_CREATE',
      'BUNKER_DIP_RECORD',
      'BUNKER_ADJUST',
      'BUNKER_TRANSFER',
      'BUNKER_UPDATE',
    ]);
    renderAt('/fuel/bunker-tanks');

    expect(await screen.findByText('BNK-DSL-01')).toBeInTheDocument();
    expect(screen.getByText('BNK-DSL-02')).toBeInTheDocument();
    expect(screen.getAllByText('Colombo Central Depot').length).toBeGreaterThan(0);
    expect(screen.getAllByText('DIESEL')).toHaveLength(2);
    expect(screen.getByText('6,500.0 L')).toBeInTheDocument();
    expect(screen.getByText('Low Stock')).toBeInTheDocument();
    expect(screen.getByText('Normal')).toBeInTheDocument();
  });

  it('renders tank detail page with authoritative book balance and physical dip observation', async () => {
    setupBunkerHandlers([
      'BUNKER_VIEW',
      'BUNKER_LEDGER_VIEW',
      'BUNKER_DIP_RECORD',
      'BUNKER_ADJUST',
      'BUNKER_TRANSFER',
    ]);
    renderAt('/fuel/bunker-tanks/tank-1');

    expect(await screen.findByText('Authoritative Book Inventory & Capacity')).toBeInTheDocument();
    expect(screen.getAllByText('6,500.0 L').length).toBeGreaterThan(0);
    expect(screen.getByText('Capacity: 10,000 L')).toBeInTheDocument();
    expect(screen.getByText('3,500 L')).toBeInTheDocument();
    expect(screen.getByText('6,480.0 L')).toBeInTheDocument();
    expect(screen.getByText('-20.0 L')).toBeInTheDocument();
  });

  it('renders "No physical dip recorded" when tank has no dip readings', async () => {
    const balanceNoDip: BunkerTankBalance = {
      ...mockBalance,
      latestDipQuantityLiters: null,
      latestDipAt: null,
      latestVarianceLiters: null,
    };
    setupBunkerHandlers(['BUNKER_VIEW', 'BUNKER_LEDGER_VIEW'], [mockTank1], balanceNoDip);
    renderAt('/fuel/bunker-tanks/tank-1');

    expect(await screen.findByText('No physical dip recorded')).toBeInTheDocument();
  });

  it('renders server-paginated stock ledger movements with readable labels', async () => {
    setupBunkerHandlers(['BUNKER_VIEW', 'BUNKER_LEDGER_VIEW']);
    renderAt('/fuel/bunker-tanks/tank-1');

    expect(await screen.findByText('Stock Movement Ledger')).toBeInTheDocument();
    expect(screen.getByText('Opening Balance')).toBeInTheDocument();
    expect(screen.getByText('Purchase Receipt')).toBeInTheDocument();
    expect(screen.getByText('Fuel Issue')).toBeInTheDocument();
    expect(screen.getByText('+2,000.0 L')).toBeInTheDocument();
    expect(screen.getByText('-500.0 L')).toBeInTheDocument();
    expect(screen.getByText('Initial commissioning balance')).toBeInTheDocument();
  });

  it('records physical dip observation without mutating book stock', async () => {
    setupBunkerHandlers(['BUNKER_VIEW', 'BUNKER_DIP_RECORD', 'BUNKER_LEDGER_VIEW']);
    const user = userEvent.setup();
    renderAt('/fuel/bunker-tanks/tank-1');

    const recordDipBtn = await screen.findByRole('button', { name: /Record Physical Dip/i });
    await user.click(recordDipBtn);

    expect(await screen.findByText('Observation Only')).toBeInTheDocument();
    expect(screen.getByText(/Recording a physical dip captures an observational measurement/i)).toBeInTheDocument();

    const dipInput = screen.getByPlaceholderText(/e.g. 5240.5/);
    await user.clear(dipInput);
    await user.type(dipInput, '6520');

    const okBtn = screen.getByRole('button', { name: 'OK' });
    await user.click(okBtn);

    await waitFor(() => {
      expect(screen.queryByText('Observation Only')).not.toBeInTheDocument();
    });
  });

  it('performs stock adjustment with explicit direction and strong audit ledger notice', async () => {
    setupBunkerHandlers(['BUNKER_VIEW', 'BUNKER_ADJUST', 'BUNKER_LEDGER_VIEW']);
    const user = userEvent.setup();
    renderAt('/fuel/bunker-tanks/tank-1');

    const adjustBtn = await screen.findByRole('button', { name: /Stock Adjustment/i });
    await user.click(adjustBtn);

    expect(await screen.findByText('Authoritative Inventory Change')).toBeInTheDocument();
    expect(screen.getByText(/A stock adjustment directly mutates the book inventory balance/i)).toBeInTheDocument();

    const increaseBtn = screen.getByText(/Stock Increase/i);
    await user.click(increaseBtn);

    const qtyInput = screen.getByPlaceholderText('Quantity in Liters');
    await user.clear(qtyInput);
    await user.type(qtyInput, '50');

    const reasonInput = screen.getByPlaceholderText(/Variance reconciliation/);
    await user.type(reasonInput, 'Calibration reconciliation after meter testing');

    const okBtn = screen.getByRole('button', { name: 'OK' });
    await user.click(okBtn);

    await waitFor(() => {
      expect(screen.queryByText('Authoritative Inventory Change')).not.toBeInTheDocument();
    });
  });

  it('performs inter-tank fuel transfer with source and destination validation', async () => {
    setupBunkerHandlers(['BUNKER_VIEW', 'BUNKER_TRANSFER', 'BUNKER_LEDGER_VIEW']);
    const user = userEvent.setup();
    renderAt('/fuel/bunker-tanks/tank-1');

    const transferBtn = await screen.findByRole('button', { name: /Transfer Fuel/i });
    await user.click(transferBtn);

    expect(await screen.findByText('Atomic Transfer')).toBeInTheDocument();

    const sourceSelect = screen.getByRole('combobox', { name: 'Source Tank' });
    fireEvent.mouseDown(sourceSelect.closest('.ant-select-selector') || sourceSelect);
    const sourceOptions = await screen.findAllByText(/BNK-DSL-01/);
    await user.click(sourceOptions[sourceOptions.length - 1]);

    const destSelect = screen.getByRole('combobox', { name: 'Destination Tank' });
    fireEvent.mouseDown(destSelect.closest('.ant-select-selector') || destSelect);
    const destOptions = await screen.findAllByText(/BNK-DSL-02/);
    await user.click(destOptions[destOptions.length - 1]);

    const qtyInput = screen.getByPlaceholderText('Quantity in Liters');
    await user.clear(qtyInput);
    await user.type(qtyInput, '500');

    const reasonInput = screen.getByPlaceholderText(/Operational tank rebalancing/);
    await user.type(reasonInput, 'Depot tank rebalancing');

    const okBtn = screen.getByRole('button', { name: 'OK' });
    await user.click(okBtn);

    await waitFor(() => {
      expect(screen.queryByText('Atomic Transfer')).not.toBeInTheDocument();
    });
  });

  it('creates new bunker tank with internal station selection', async () => {
    setupBunkerHandlers(['BUNKER_VIEW', 'BUNKER_CREATE']);
    const user = userEvent.setup();
    renderAt('/fuel/bunker-tanks');

    const newTankBtn = await screen.findByRole('button', { name: /New Tank/i });
    await user.click(newTankBtn);

    expect(await screen.findByText('Create New Bunker Tank')).toBeInTheDocument();

    const stationSelect = screen.getByRole('combobox', { name: 'Internal Fuel Station' });
    fireEvent.mouseDown(stationSelect.closest('.ant-select-selector') || stationSelect);
    const stationOptions = await screen.findAllByText(/Colombo Central Depot/);
    await user.click(stationOptions[stationOptions.length - 1]);

    const tankCodeInput = screen.getByPlaceholderText('BNK-DSL-01');
    await user.type(tankCodeInput, 'BNK-PET-01');

    const fuelTypeSelect = screen.getByRole('combobox', { name: 'Fuel Type' });
    fireEvent.mouseDown(fuelTypeSelect.closest('.ant-select-selector') || fuelTypeSelect);
    const fuelOptions = await screen.findAllByText('PETROL');
    await user.click(fuelOptions[fuelOptions.length - 1]);

    const tankNameInput = screen.getByPlaceholderText('Main Depot Diesel Tank');
    await user.type(tankNameInput, 'Depot Petrol Tank');

    const capacityInput = screen.getByPlaceholderText('10000');
    await user.clear(capacityInput);
    await user.type(capacityInput, '15000');

    const okBtn = screen.getByRole('button', { name: 'OK' });
    await user.click(okBtn);

    await waitFor(() => {
      expect(screen.queryByText('Create New Bunker Tank')).not.toBeInTheDocument();
    });
  });

  it('hides unauthorized actions when user has only BUNKER_VIEW permission', async () => {
    setupBunkerHandlers(['BUNKER_VIEW']);
    renderAt('/fuel/bunker-tanks');

    await screen.findByText('BNK-DSL-01');
    expect(screen.queryByRole('button', { name: /New Tank/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Inter-Tank Transfer/i })).not.toBeInTheDocument();
  });

  it('displays business errors when backend rejects adjustment or transfer', async () => {
    setupBunkerHandlers(['BUNKER_VIEW', 'BUNKER_ADJUST']);
    server.use(
      http.post('*/bunker-tanks/:id/adjustments', () =>
        HttpResponse.json(
          {
            code: 'INSUFFICIENT_BUNKER_STOCK',
            message: 'Stock adjustment exceeds available bunker inventory',
          },
          { status: 400 }
        )
      )
    );

    const user = userEvent.setup();
    renderAt('/fuel/bunker-tanks/tank-1');

    const adjustBtn = await screen.findByRole('button', { name: /Stock Adjustment/i });
    await user.click(adjustBtn);

    const qtyInput = await screen.findByPlaceholderText('Quantity in Liters');
    await user.clear(qtyInput);
    await user.type(qtyInput, '9000');

    const reasonInput = screen.getByPlaceholderText(/Variance reconciliation/);
    await user.type(reasonInput, 'Attempted overdraw adjustment');

    const okBtn = screen.getByRole('button', { name: 'OK' });
    await user.click(okBtn);

    expect(
      await screen.findByText('Stock adjustment exceeds available bunker inventory')
    ).toBeInTheDocument();
  });

  it('guards bunker routes without BUNKER_VIEW permission', async () => {
    setupBunkerHandlers([]);
    renderAt('/fuel/bunker-tanks');

    expect(
      await screen.findByText('Select an available module from the navigation.')
    ).toBeInTheDocument();
  });
});