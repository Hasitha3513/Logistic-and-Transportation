import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import VehicleMaintenanceSection from './VehicleMaintenanceSection';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: (perm: string) => perm === 'VEHICLE_MAINTENANCE_MANAGE' || perm === 'VEHICLE_VIEW',
    user: { username: 'fleet.manager' },
  }),
}));

const mockGet = vi.fn();
const mockPost = vi.fn();
const mockPatch = vi.fn();

vi.mock('../api/client', () => ({
  api: {
    get: (url: string) => mockGet(url),
    post: (url: string, data: unknown) => mockPost(url, data),
    patch: (url: string, data: unknown) => mockPatch(url, data),
  },
}));

function renderComponent(vehicleId: string = 'test-vehicle-123') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AntApp>
        <VehicleMaintenanceSection vehicleId={vehicleId} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('VehicleMaintenanceSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders maintenance section header and schedules table', async () => {
    mockGet.mockResolvedValueOnce({
      data: [
        {
          id: 'm-1',
          vehicleId: 'test-vehicle-123',
          maintenanceType: '50,000 km Service',
          scheduledStart: '2026-09-01T08:00:00Z',
          scheduledEnd: '2026-09-01T16:00:00Z',
          status: 'SCHEDULED',
          description: 'Full fluid flush and inspection',
          serviceProvider: 'Central Workshop',
          cost: 350.0,
          createdAt: '2026-08-19T10:00:00Z',
          updatedAt: '2026-08-19T10:00:00Z',
        },
      ],
    });

    renderComponent();

    expect(screen.getByText('Scheduled maintenance')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('50,000 km Service')).toBeDefined();
      expect(screen.getByText('Scheduled')).toBeDefined();
      expect(screen.getByText('Central Workshop')).toBeDefined();
      expect(screen.getByText('$350.00')).toBeDefined();
      expect(screen.getByText('Reschedule')).toBeDefined();
      expect(screen.getByText('Complete')).toBeDefined();
      expect(screen.getByText('Cancel')).toBeDefined();
    });
  });

  it('renders empty table when no maintenance schedules exist', async () => {
    mockGet.mockResolvedValueOnce({ data: [] });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('No maintenance schedules found')).toBeDefined();
    });
  });

  it('renders error alert when api call fails', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network error'));

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Maintenance schedules could not be loaded')).toBeDefined();
    });
  });
});
