import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { RouteRevisionSection } from './RouteRevisionSection';

const mockGet = vi.fn();

vi.mock('../../../api/client', () => ({
  api: {
    get: (url: string) => mockGet(url),
    post: vi.fn(),
  },
}));

function renderComponent(routeId: string = 'route-100') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <AntApp>
        <RouteRevisionSection routeId={routeId} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('RouteRevisionSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders route revisions with version badges and details', async () => {
    mockGet.mockResolvedValueOnce({
      data: [
        {
          id: 'rev-2',
          routeId: 'route-100',
          revisionNumber: 2,
          code: 'RT-100',
          name: 'Main Corridor v2',
          originLocationId: 'loc-1',
          destinationLocationId: 'loc-2',
          plannedDistanceKm: 140.0,
          estimatedDurationMinutes: 110,
          active: true,
          stopLocationIds: ['stop-1', 'stop-2'],
          changedAt: '2026-08-24T12:00:00Z',
          changedBy: 'planner_admin',
        },
        {
          id: 'rev-1',
          routeId: 'route-100',
          revisionNumber: 1,
          code: 'RT-100',
          name: 'Main Corridor',
          originLocationId: 'loc-1',
          destinationLocationId: 'loc-2',
          plannedDistanceKm: 150.0,
          estimatedDurationMinutes: 120,
          active: true,
          stopLocationIds: ['stop-1'],
          changedAt: '2026-08-20T10:00:00Z',
          changedBy: 'system',
        },
      ],
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Revision History \(2\)/)).toBeInTheDocument();
      expect(screen.getByText('Main Corridor v2')).toBeInTheDocument();
      expect(screen.getByText('Main Corridor')).toBeInTheDocument();
      expect(screen.getByText('planner_admin')).toBeInTheDocument();
      expect(screen.getByText('140 km')).toBeInTheDocument();
    });
  });

  it('renders empty state when no revisions exist', async () => {
    mockGet.mockResolvedValueOnce({ data: [] });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('No revisions recorded yet')).toBeInTheDocument();
    });
  });
});
