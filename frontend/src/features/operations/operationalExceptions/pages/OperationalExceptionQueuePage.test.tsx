import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import OperationalExceptionQueuePage from './OperationalExceptionQueuePage';

vi.mock('../../../../auth/AuthContext', () => ({
  useAuth: () => ({ hasPermission: () => true }),
}));

vi.mock('../api/operationalExceptionApi', () => ({
  operationalExceptionApi: {
    list: vi.fn().mockResolvedValue({
      content: [{
        id: '78000000-0000-0000-0000-000000000078', caseReference: 'OEX-0123456789AB',
        sourceModule: 'ROUTING', sourceType: 'ACCIDENT', sourceId: '22000000-0000-0000-0000-000000000022',
        occurredAt: '2026-09-04T00:00:00Z', summaryCode: 'ROUTE_DISRUPTION_CREATED', category: 'SAFETY',
        severity: 'HIGH', status: 'OPEN', slaStatus: 'ON_TRACK', responseDueAt: '2026-09-04T01:00:00Z',
        resolutionDueAt: '2026-09-04T08:00:00Z', assignmentType: 'ROLE_QUEUE',
        assignedRoleCode: 'OPERATIONS_SAFETY_QUEUE', escalationLevel: 'L0', version: 0,
        createdAt: '2026-09-04T00:00:00Z', updatedAt: '2026-09-04T00:00:00Z',
      }], page: 0, size: 20, totalElements: 1, totalPages: 1,
    }),
    get: vi.fn(), history: vi.fn(), command: vi.fn(), actionCommand: vi.fn(),
  },
}));

describe('OperationalExceptionQueuePage', () => {
  it('renders the safe cross-domain queue and SLA facts', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<MemoryRouter><QueryClientProvider client={client}>
      <OperationalExceptionQueuePage />
    </QueryClientProvider></MemoryRouter>);
    expect(await screen.findByText('OEX-0123456789AB')).toBeDefined();
    expect(screen.getByText('ROUTING · ROUTE_DISRUPTION_CREATED')).toBeDefined();
    expect(screen.getByText('OPERATIONS_SAFETY_QUEUE')).toBeDefined();
    expect(screen.getByText('ON_TRACK')).toBeDefined();
  });
});
