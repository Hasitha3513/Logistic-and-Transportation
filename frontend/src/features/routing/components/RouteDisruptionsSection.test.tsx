import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { RouteDisruptionsSection } from './RouteDisruptionsSection';

const mockGet = vi.fn();
const mockPost = vi.fn();

vi.mock('../../../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: (perm: string) => perm === 'ROUTE_DISRUPTION_MANAGE' || perm === 'ROUTE_VIEW' || perm === 'ROUTE_UPDATE',
    user: { username: 'traffic.controller' },
  }),
}));

vi.mock('../../../api/client', () => ({
  api: {
    get: (url: string) => mockGet(url),
    post: (url: string, data: unknown) => mockPost(url, data),
  },
}));

function renderComponent(routeId: string = 'route-100') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <AntApp>
        <RouteDisruptionsSection routeId={routeId} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('RouteDisruptionsSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders route disruptions with severity and type badges', async () => {
    mockGet.mockResolvedValueOnce({
      data: [
        {
          id: 'dis-1',
          routeId: 'route-100',
          disruptionType: 'ROAD_CLOSURE',
          severity: 'HIGH',
          description: 'Emergency bridge repair on section 3',
          effectiveFrom: '2026-08-24T08:00:00Z',
          effectiveUntil: null,
          detourRouteId: 'detour-route-1',
          status: 'ACTIVE',
          createdAt: '2026-08-24T08:00:00Z',
          createdBy: 'traffic_controller',
          resolvedAt: null,
          resolvedBy: null,
        },
      ],
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Route Disruptions \(1\)/)).toBeInTheDocument();
      expect(screen.getByText('Emergency bridge repair on section 3')).toBeInTheDocument();
      expect(screen.getByText('HIGH')).toBeInTheDocument();
      expect(screen.getByText('ACTIVE')).toBeInTheDocument();
      expect(screen.getByTestId('resolve-disruption-btn-dis-1')).toBeInTheDocument();
    });
  });

  it('opens report disruption modal when Report Disruption button is clicked', async () => {
    mockGet.mockResolvedValueOnce({ data: [] });
    // Detours query
    mockGet.mockResolvedValueOnce({ data: [] });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId('report-disruption-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('report-disruption-btn'));

    await waitFor(() => {
      expect(screen.getByText('Report Route Disruption')).toBeInTheDocument();
      expect(screen.getByText('Record Disruption')).toBeInTheDocument();
    });
  });
});
