import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import DriverViolationsSection from './DriverViolationsSection';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: (perm: string) => perm === 'DRIVER_VIOLATION_MANAGE' || perm === 'DRIVER_VIEW',
    user: { username: 'fleet.manager' },
  }),
}));

const mockGet = vi.fn();
const mockPost = vi.fn();

vi.mock('../api/client', () => ({
  api: {
    get: (url: string) => mockGet(url),
    post: (url: string, data: any) => mockPost(url, data),
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
        <DriverViolationsSection driverId={driverId} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('DriverViolationsSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders driver violations section and table', async () => {
    mockGet.mockResolvedValueOnce({
      data: [
        {
          id: 'v-1',
          driverId: 'test-driver-123',
          violationType: 'SPEEDING',
          severity: 'MODERATE',
          violationDate: '2026-09-01T10:00:00Z',
          penaltyPoints: 3,
          fineAmount: 150.0,
          paymentStatus: 'UNPAID',
          location: 'Highway 101',
          description: 'Speeding 20km/h over',
          createdAt: '2026-08-19T10:00:00Z',
          updatedAt: '2026-08-19T10:00:00Z',
        },
      ],
    });

    renderComponent();

    expect(screen.getByText('Traffic Violations & Infractions')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('SPEEDING')).toBeDefined();
      expect(screen.getByText('MODERATE')).toBeDefined();
      expect(screen.getByText('$150.00')).toBeDefined();
      expect(screen.getByText('UNPAID')).toBeDefined();
      expect(screen.getByText('Highway 101')).toBeDefined();
      expect(screen.getByText('Pay')).toBeDefined();
      expect(screen.getByText('Waive')).toBeDefined();
      expect(screen.getByText('Dispute')).toBeDefined();
    });
  });
});
