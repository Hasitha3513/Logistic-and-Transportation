import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import FreightReportsPage from './FreightReportsPage';

const hasPermission = vi.fn();
vi.mock('../../../../auth/AuthContext', () => ({ useAuth: () => ({ hasPermission }) }));
vi.mock('../hooks/useFreightReports', () => ({
  useFreightReportSummary: () => ({ data: { freightOrders: 2, manifests: 1, loadPlans: 1, claims: 1,
    cargoExceptions: 1, complianceOutcomes: { PASS: 0, FAIL: 0, INCOMPLETE: 1 } }, isError: false }),
  useFreightReportShipments: () => ({ data: { content: [{ freightOrderId: 'order-1', orderNumber: 'FO-001',
    customerId: 'customer-1', manifestNumber: 'CM-001', loadPlanNumber: 'LP-001', cargoWeightKg: 125,
    complianceOutcome: 'INCOMPLETE', incompleteDiagnostics: ['VEHICLE_VOLUME_CAPACITY_UNAVAILABLE'],
    createdAt: '2026-08-01T00:00:00Z' }], totalElements: 1 }, isLoading: false, isError: false }),
}));

describe('FreightReportsPage', () => {
  beforeEach(() => hasPermission.mockImplementation((permission: string) => permission === 'FREIGHT_REPORT_VIEW'));

  it('renders source-backed metrics, shipment rows and honest incomplete state', () => {
    render(<MemoryRouter><FreightReportsPage /></MemoryRouter>);
    expect(screen.getByText('FO-001')).toBeInTheDocument();
    expect(screen.getByText('CM-001')).toBeInTheDocument();
    expect(screen.getAllByText('INCOMPLETE').length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: /export csv/i })).not.toBeInTheDocument();
  });

  it('shows export only with the dedicated permission', () => {
    hasPermission.mockReturnValue(true);
    render(<MemoryRouter><FreightReportsPage /></MemoryRouter>);
    expect(screen.getByRole('button', { name: /export csv/i })).toBeInTheDocument();
  });
});
