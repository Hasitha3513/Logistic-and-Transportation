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
import type { ClaimResponse, PolicyResponse } from '../types/insurance';

const policyId = '11111111-1111-4111-8111-111111111111';
const claimId = '22222222-2222-4222-8222-222222222222';
const freightOrderId = '33333333-3333-4333-8333-333333333333';

const mockPolicy: PolicyResponse = {
  id: policyId,
  policyNumber: 'POL-2026-000001',
  freightOrderId: freightOrderId,
  insuranceProvider: 'Zurich Freight Mutual',
  policyType: 'ALL_RISK',
  coverageAmount: 50000,
  premiumAmount: 500,
  deductibleAmount: 250,
  currencyCode: 'USD',
  validFrom: '2026-01-01T00:00:00Z',
  validUntil: '2026-12-31T23:59:59Z',
  status: 'ACTIVE',
  termsAndConditions: 'All risks of physical loss or damage to cargo covered.',
  version: 0,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  createdBy: 'officer',
  updatedBy: 'officer',
};

const mockClaim: ClaimResponse = {
  id: claimId,
  claimNumber: 'CLM-2026-000001',
  policyId: policyId,
  incidentDate: '2026-02-15T10:00:00Z',
  description: 'Water ingress damaged pallet electronics',
  claimedAmount: 12000,
  assessedAmount: 10000,
  totalSettledAmount: 4000,
  currencyCode: 'USD',
  status: 'APPROVED',
  assessmentNotes: 'Surveyor inspected, approved 10k payable',
  version: 2,
  createdAt: '2026-02-16T00:00:00Z',
  updatedAt: '2026-02-17T00:00:00Z',
  createdBy: 'officer',
  updatedBy: 'officer',
  settlements: [
    {
      id: '44444444-4444-4444-8444-444444444444',
      settlementReference: 'STL-2026-000001-001',
      settledAmount: 4000,
      currencyCode: 'USD',
      settlementNotes: 'Advance wire tranche 1',
      settledBy: 'finance_manager',
      settledAt: '2026-02-18T10:00:00Z',
    },
  ],
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
    http.get('*/v1/freight/insurance/policies', () => HttpResponse.json([mockPolicy])),
    http.get('*/v1/freight/insurance/policies/:id', () => HttpResponse.json(mockPolicy)),
    http.get('*/v1/freight/insurance/claims', () => HttpResponse.json([mockClaim])),
    http.get('*/v1/freight/insurance/claims/:id', () => HttpResponse.json(mockClaim)),
    http.get('*/v1/freight/insurance/claims/by-policy/:policyId', () => HttpResponse.json([mockClaim]))
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

describe('Freight Insurance Pages', () => {
  it('renders policy list page with policies table', async () => {
    handlers(['CARGO_INSURANCE_VIEW']);
    renderAt('/freight/insurance/policies');

    expect(await screen.findByText('POL-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('Zurich Freight Mutual')).toBeInTheDocument();
    expect(screen.getByText('50,000 USD')).toBeInTheDocument();
  });

  it('renders policy details page with associated claims', async () => {
    handlers(['CARGO_INSURANCE_VIEW', 'CARGO_INSURANCE_MANAGE']);
    renderAt(`/freight/insurance/policies/${policyId}`);

    expect(await screen.findByText(/Policy: POL-2026-000001/i)).toBeInTheDocument();
    expect(screen.getByText('Zurich Freight Mutual')).toBeInTheDocument();
    expect(screen.getByText('CLM-2026-000001')).toBeInTheDocument();
  });

  it('renders claim list page with claims table', async () => {
    handlers(['CARGO_INSURANCE_VIEW']);
    renderAt('/freight/insurance/claims');

    expect(await screen.findByText('CLM-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('12,000 USD')).toBeInTheDocument();
    expect(screen.getByText('10,000 USD')).toBeInTheDocument();
    expect(screen.getByText('APPROVED')).toBeInTheDocument();
  });

  it('renders claim details with settlement history and settlement action', async () => {
    handlers(['CARGO_INSURANCE_VIEW', 'CARGO_INSURANCE_MANAGE']);
    const user = userEvent.setup();
    renderAt(`/freight/insurance/claims/${claimId}`);

    expect(await screen.findByText(/Claim: CLM-2026-000001/i)).toBeInTheDocument();
    expect(screen.getByText('Advance wire tranche 1')).toBeInTheDocument();
    expect(screen.getByText('STL-2026-000001-001')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Record Settlement/i })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Record Settlement/i }));
    expect(await screen.findByText('Record Settlement Payment')).toBeInTheDocument();
  });

  it('guards insurance routes when missing permissions', async () => {
    handlers([]);
    renderAt('/freight/insurance/policies');
    expect(await screen.findByText('Select an available module from the navigation.')).toBeInTheDocument();
  });
});
