import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { RoutePerformanceSection } from './RoutePerformanceSection';

const mockGet = vi.fn();

vi.mock('../../../api/client', () => ({
  api: {
    get: (url: string, config?: unknown) => mockGet(url, config),
    post: vi.fn(),
  },
}));

function renderComponent(routeId: string = 'route-perf-1') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <AntApp>
        <RoutePerformanceSection routeId={routeId} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('RoutePerformanceSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders operational KPIs and variance analysis', async () => {
    mockGet.mockResolvedValueOnce({
      data: {
        routeId: 'route-perf-1',
        routeCode: 'RT-100',
        routeName: 'Colombo - Kandy',
        totalTripCount: 10,
        completedTripCount: 8,
        plannedDistanceKm: 120.0,
        averageActualDistanceKm: 125.0,
        distanceVarianceKm: 5.0,
        distanceVariancePercent: 4.17,
        plannedDurationMinutes: 180,
        averageActualDurationMinutes: 195,
        durationVarianceMinutes: 15,
        durationVariancePercent: 8.33,
        onTimeTripCount: 6,
        delayedTripCount: 2,
        averageDelayMinutes: 22.5,
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Route Operational Performance/i)).toBeInTheDocument();
      expect(screen.getByText('10')).toBeInTheDocument(); // Total Trips
      expect(screen.getByText('8')).toBeInTheDocument();  // Completed Trips
      expect(screen.getByText('75%')).toBeInTheDocument(); // On-Time Rate: 6/8 = 75%
      expect(screen.getByText(/Variance Analysis/i)).toBeInTheDocument();
      expect(screen.getByText(/125 km/i)).toBeInTheDocument();
      expect(screen.getByText(/6 On-Time/i)).toBeInTheDocument();
      expect(screen.getByText(/2 Delayed/i)).toBeInTheDocument();
    });
  });

  it('shows empty state when no trips have occurred for the route', async () => {
    mockGet.mockResolvedValueOnce({
      data: {
        routeId: 'route-perf-empty',
        routeCode: 'RT-EMPTY',
        routeName: 'Unused Route',
        totalTripCount: 0,
        completedTripCount: 0,
        plannedDistanceKm: 50.0,
        plannedDurationMinutes: 60,
        onTimeTripCount: 0,
        delayedTripCount: 0,
      },
    });

    renderComponent('route-perf-empty');

    await waitFor(() => {
      expect(screen.getByText(/No trip operations recorded for this route yet/i)).toBeInTheDocument();
    });
  });

  it('renders an accessible loading state while analytics are pending', () => {
    mockGet.mockReturnValueOnce(new Promise(() => undefined));

    renderComponent();

    expect(screen.getByLabelText('Loading performance analytics')).toBeInTheDocument();
  });

  it('renders an operational error without inventing fallback metrics', async () => {
    mockGet.mockRejectedValueOnce(new Error('analytics unavailable'));

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Route performance analytics could not be loaded/i)).toBeInTheDocument();
    });
    expect(screen.queryByText('Total Trips')).not.toBeInTheDocument();
  });
});
