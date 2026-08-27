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
import type { CargoException } from '../types';

const exceptionId = '11111111-2222-3333-4444-555555555555';
const freightOrderId = '66666666-7777-8888-9999-000000000000';

const mockException: CargoException = {
  id: exceptionId,
  exceptionNumber: 'CEX-2026-000001',
  exceptionType: 'DAMAGE',
  status: 'OPEN',
  severity: 'HIGH',
  freightOrderId: freightOrderId,
  manifestId: null,
  manifestItemId: null,
  description: 'Pallet crushed during cross-dock forklift handling',
  impact: 'Outer carton crushed, inner packaging intact',
  restriction: 'Hold for inspection at Bay 3',
  correctiveAction: 'Repackage into standard crate',
  resolution: null,
  resolvedAt: null,
  resolvedBy: null,
  history: [
    {
      id: 'aaaa1111-2222-3333-4444-555555555555',
      action: 'HOLD_APPLIED',
      actor: 'freight_officer',
      occurredAt: '2026-02-20T10:00:00Z',
      reason: 'Physical box deformation detected',
      details: 'Hold for inspection at Bay 3',
    },
  ],
  createdAt: '2026-02-20T09:30:00Z',
  updatedAt: '2026-02-20T10:00:00Z',
  createdBy: 'freight_officer',
  updatedBy: 'freight_officer',
  version: 1,
};

function handlers(permissions: string[]) {
  server.use(
    http.get('*/auth/me', () =>
      HttpResponse.json({
        id: 'u',
        username: 'officer',
        firstName: 'Cargo',
        lastName: 'Officer',
        active: true,
        roles: [],
        permissions,
      })
    ),
    http.get('*/v1/freight/exceptions', () => HttpResponse.json([mockException])),
    http.get('*/v1/freight/exceptions/:id', () => HttpResponse.json(mockException))
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

describe('Cargo Exception Pages', () => {
  it('renders exception list page with exceptions table', async () => {
    handlers(['CARGO_EXCEPTION_VIEW']);
    renderAt('/freight/exceptions');

    expect(await screen.findByText('CEX-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('Damage')).toBeInTheDocument();
    expect(screen.getByText('HIGH')).toBeInTheDocument();
    expect(screen.getByText('OPEN')).toBeInTheDocument();
  });

  it('renders exception details page with history and action buttons', async () => {
    handlers(['CARGO_EXCEPTION_VIEW', 'CARGO_EXCEPTION_MANAGE']);
    const user = userEvent.setup();
    renderAt(`/freight/exceptions/${exceptionId}`);

    expect(await screen.findByText('CEX-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('Pallet crushed during cross-dock forklift handling')).toBeInTheDocument();
    expect(screen.getByText('HOLD_APPLIED')).toBeInTheDocument();
    expect(screen.getByText('Physical box deformation detected')).toBeInTheDocument();

    const resolveBtn = screen.getByRole('button', { name: /Resolve/i });
    expect(resolveBtn).toBeInTheDocument();

    await user.click(resolveBtn);
    expect(await screen.findByText('Resolution Summary')).toBeInTheDocument();
  });

  it('guards cargo exception routes when missing permissions', async () => {
    handlers([]);
    renderAt('/freight/exceptions');
    expect(await screen.findByText('Select an available module from the navigation.')).toBeInTheDocument();
  });
});
