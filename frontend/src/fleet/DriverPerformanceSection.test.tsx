import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import DriverPerformanceSection from './DriverPerformanceSection';

const mockGet = vi.fn();

vi.mock('../api/client', () => ({
  api: {
    get: (url: string) => mockGet(url),
  },
}));

function renderComponent(driverId: string = 'test-driver-123') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AntApp>
        <DriverPerformanceSection driverId={driverId} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('DriverPerformanceSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders driver performance scorecard and metrics', async () => {
    mockGet.mockResolvedValueOnce({
      data: {
        driverId: 'test-driver-123',
        driverName: 'Alice Cooper',
        totalTripsAssigned: 20,
        totalTripsCompleted: 19,
        totalTripsCancelled: 1,
        tripCompletionRate: 95.0,
        totalViolations: 1,
        totalPenaltyPoints: 2,
        criticalViolations: 0,
        totalFines: 100.0,
        unpaidFines: 0.0,
        safetyScore: 92,
        overallRating: 'EXCELLENT',
        evaluatedAt: '2026-08-19T10:00:00Z',
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Driver Performance Scorecard')).toBeDefined();
      expect(screen.getByText('EXCELLENT')).toBeDefined();
      expect(screen.getByText('Safety Score')).toBeDefined();
      expect(screen.getByText('Trip Reliability')).toBeDefined();
      expect(screen.getByText('Compliance & Penalties')).toBeDefined();
      expect(screen.getByText('92/100')).toBeDefined();
      expect(screen.getByText('95')).toBeDefined();
    });
  });
});
