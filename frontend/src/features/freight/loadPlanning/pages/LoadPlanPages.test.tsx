import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import App from '../../../../App';
import { appTheme } from '../../../../app/theme/theme';
import { AuthProvider } from '../../../../auth/AuthContext';
import { server } from '../../../../test/server';
import type { LoadPlan } from '../types/loadPlan';

const planId = '77777777-7777-4777-8777-777777777777';
const manifestId = '66666666-6666-4666-8666-666666666666';
const vehicleId = '88888888-8888-4888-8888-888888888888';

const loadPlan: LoadPlan = {
  id: planId,
  loadPlanNumber: 'LP-2026-000001',
  cargoManifestId: manifestId,
  vehicleId: vehicleId,
  placements: [
    {
      id: '99999999-9999-4999-8999-999999999999',
      manifestItemId: '55555555-5555-4555-8555-555555555555',
      placementOrder: 0,
      zoneReference: 'FRONT',
      stackGroup: 'STACK-1',
      containerReference: 'PALLET-1',
      loadingSequence: 1,
      specialHandlingNotes: 'Fragile equipment',
    },
  ],
  notes: 'Planning test notes',
  readinessStatus: 'DRAFT',
  readyAt: null,
  readyBy: null,
  version: 0,
  createdAt: '2026-08-25T00:00:00Z',
  updatedAt: '2026-08-25T00:00:00Z',
  createdBy: 'manager',
  updatedBy: 'manager',
};

function handlers(permissions: string[], plan: LoadPlan = loadPlan) {
  let currentPlan = { ...plan };
  server.use(
    http.get('*/auth/me', () =>
      HttpResponse.json({
        id: 'u',
        username: 'manager',
        firstName: 'Load',
        lastName: 'Manager',
        active: true,
        roles: [],
        permissions,
      })
    ),
    http.get(`*/v1/freight/load-plans/${plan.id}`, () => HttpResponse.json(currentPlan)),
    http.get('*/v1/freight/load-plans', () => HttpResponse.json([currentPlan])),
    http.post('*/v1/freight/load-plans/:id/ready', async () => {
      currentPlan = {
        ...currentPlan,
        readinessStatus: 'STRUCTURALLY_READY',
        readyAt: '2026-08-27T10:00:00Z',
        readyBy: 'manager',
        version: currentPlan.version + 1,
      };
      return HttpResponse.json(currentPlan);
    }),
    http.post('*/v1/freight/load-plans/:id/validate-layout', () =>
      HttpResponse.json({ valid: true, violations: [] })
    ),
    http.post('*/v1/freight/load-plans/:id/validate-weight-volume', () =>
      HttpResponse.json({
        loadPlanId: plan.id,
        validatedAt: '2026-08-25T00:00:00Z',
        validatedBy: 'manager',
        overallOutcome: 'INCOMPLETE',
        payloadResult: 'INCOMPLETE',
        volumeResult: 'INCOMPLETE',
        axleResult: 'INCOMPLETE',
        violations: [
          {
            code: 'LOAD_WEIGHT_DATA_MISSING',
            message: 'Cargo item weight measurements are unavailable to compute gross weight',
          },
        ],
        missingData: ['CARGO_ITEM_WEIGHT_DATA_MISSING'],
      })
    ),
    http.get('*/v1/freight/manifests', () =>
      HttpResponse.json({
        content: [
          {
            id: manifestId,
            manifestNumber: 'CM-2026-000001',
            freightOrderNumber: 'FO-2026-000001',
            finalized: true,
            items: [],
          },
        ],
        page: 0,
        limit: 100,
        totalElements: 1,
        totalPages: 1,
      })
    ),
    http.get('*/vehicles', () =>
      HttpResponse.json([
        {
          id: vehicleId,
          registrationNumber: 'TRK-2000',
          capacityKg: 15000,
          active: true,
        },
      ])
    )
  );
}

function renderAt(path: string) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
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

describe('Load planning pages', () => {
  it('renders permission-aware load plan list with readiness status', async () => {
    handlers(['LOAD_PLAN_VIEW']);
    renderAt('/freight/load-plans');

    expect(await screen.findByText('LP-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'New load plan' })).not.toBeInTheDocument();
  });

  it('renders details page with layout and US-27 weight/volume validation triggers', async () => {
    handlers(['LOAD_PLAN_VIEW', 'LOAD_PLAN_MANAGE']);
    const user = userEvent.setup();
    renderAt(`/freight/load-plans/${planId}`);

    expect((await screen.findAllByText('LP-2026-000001')).length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: /Mark Structurally Ready/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Validate Layout/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Validate Weight & Volume/i })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Validate Layout/i }));
    expect(await screen.findByText(/All cargo items are placed/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Validate Weight & Volume/i }));
    expect(await screen.findByText(/Weight, Volume & Capacity Validation/i)).toBeInTheDocument();
    expect(screen.getAllByText('INCOMPLETE').length).toBeGreaterThan(0);
    expect(screen.getByText('CARGO_ITEM_WEIGHT_DATA_MISSING')).toBeInTheDocument();
  });

  it('marks load plan structurally ready and reflects updated status and audit', async () => {
    handlers(['LOAD_PLAN_VIEW', 'LOAD_PLAN_MANAGE']);
    const user = userEvent.setup();
    renderAt(`/freight/load-plans/${planId}`);

    expect((await screen.findAllByText('LP-2026-000001')).length).toBeGreaterThan(0);
    const readyBtn = screen.getByRole('button', { name: /Mark Structurally Ready/i });
    expect(readyBtn).toBeEnabled();

    await user.click(readyBtn);
    expect(await screen.findByText('STRUCTURALLY READY')).toBeInTheDocument();
    expect(screen.getAllByText('manager').length).toBeGreaterThanOrEqual(2);
  });

  it('hides ready and validation actions for view-only users', async () => {
    handlers(['LOAD_PLAN_VIEW']);
    renderAt(`/freight/load-plans/${planId}`);

    expect((await screen.findAllByText('LP-2026-000001')).length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: /Mark Structurally Ready/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Validate Layout/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Validate Weight & Volume/i })).not.toBeInTheDocument();
  });

  it('guards the load plans route without view permission', async () => {
    handlers([]);
    renderAt('/freight/load-plans');
    expect(await screen.findByText('Select an available module from the navigation.')).toBeInTheDocument();
  });
});
