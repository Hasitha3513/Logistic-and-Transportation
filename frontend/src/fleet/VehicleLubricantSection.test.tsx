import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { VehicleLubricantSection } from './VehicleLubricantSection';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: (perm: string) => perm === 'LUBRICANT_LOG_MANAGE' || perm === 'LUBRICANT_LOG_VIEW',
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

function renderComponent(vehicleId: string = 'test-vehicle-123') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AntApp>
        <VehicleLubricantSection vehicleId={vehicleId} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('VehicleLubricantSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders lubricant logs table with records', async () => {
    mockGet.mockResolvedValueOnce({
      data: [
        {
          id: 'lub-1',
          vehicleId: 'test-vehicle-123',
          fluidType: 'ENGINE_OIL',
          quantity: 12.5,
          unit: 'LITRE',
          recordedAt: '2026-02-15T10:00:00Z',
          odometerKm: 45000,
          engineHours: 1200,
          vendorId: null,
          supplierName: 'Mobil Lubricants',
          referenceNumber: 'REF-LUB-001',
          remarks: 'Oil change service',
          active: true,
          createdAt: '2026-02-15T10:00:00Z',
          updatedAt: '2026-02-15T10:00:00Z',
          createdBy: 'mechanic',
          updatedBy: 'mechanic',
        },
      ],
    });

    renderComponent();

    expect(screen.getByText(/Lubricant & Fluid Consumption Logs/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Engine Oil')).toBeInTheDocument();
      expect(screen.getByText(/12.5 litre/i)).toBeInTheDocument();
      expect(screen.getByText('Mobil Lubricants')).toBeInTheDocument();
      expect(screen.getByText('REF-LUB-001')).toBeInTheDocument();
    });
  });

  it('renders empty state when no logs exist', async () => {
    mockGet.mockResolvedValueOnce({ data: [] });

    renderComponent();

    await waitFor(() => {
      expect(
        screen.getByText('No lubricant or fluid consumption logs recorded for this vehicle.')
      ).toBeInTheDocument();
    });
  });

  it('renders Add Lubricant / Fluid Log button when authorized', async () => {
    mockGet.mockResolvedValueOnce({ data: [] });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Add Lubricant \/ Fluid Log/i })).toBeInTheDocument();
    });
  });
});
