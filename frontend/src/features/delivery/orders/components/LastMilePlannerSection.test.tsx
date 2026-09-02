import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LastMilePlannerSection } from './LastMilePlannerSection';

let hasPermission = vi.fn(() => true);

vi.mock('../../../../auth/AuthContext', () => ({
  useAuth: () => ({ hasPermission }),
}));

vi.mock('../../../../api/client', () => ({
  api: {
    get: vi.fn(() => Promise.resolve({ data: {
      deliveryId: '03cd51bf-7ae3-44bd-8202-817fef87341d',
      failedAttemptCount: 1,
      activeExceptionCount: 1,
      openEscalationCount: 0,
      availableActions: ['SCHEDULE_REDELIVERY', 'RECALCULATE_ETA'],
    } })),
  },
}));

function renderPlanner() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<MemoryRouter><QueryClientProvider client={client}><AntApp>
    <LastMilePlannerSection deliveryId="03cd51bf-7ae3-44bd-8202-817fef87341d" />
  </AntApp></QueryClientProvider></MemoryRouter>);
}

describe('LastMilePlannerSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    hasPermission = vi.fn(() => true);
  });

  it('renders read-only planner context and routes users to existing actions', async () => {
    renderPlanner();
    expect(await screen.findByText('Failed attempts')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Schedule redelivery' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Recalculate ETA' })).toBeInTheDocument();
  });

  it('does not expose the planner to users without either owning view permission', () => {
    hasPermission = vi.fn(() => false);
    const { container } = renderPlanner();
    expect(container.querySelector('#last-mile-planner')).toBeNull();
  });
});
