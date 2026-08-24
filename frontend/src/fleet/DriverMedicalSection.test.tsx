import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DriverMedicalSection } from './DriverMedicalSection';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: (perm: string) => perm === 'DRIVER_MEDICAL_MANAGE' || perm === 'DRIVER_MEDICAL_VIEW',
    user: { username: 'fleet.manager' },
  }),
}));

const mockGet = vi.fn();
const mockPost = vi.fn();

vi.mock('../api/client', () => ({
  api: {
    get: (url: string) => mockGet(url),
    post: (url: string, data: unknown) => mockPost(url, data),
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
        <DriverMedicalSection driverId={driverId} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('DriverMedicalSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders driver medical section and records table', async () => {
    mockGet.mockResolvedValueOnce({
      data: [
        {
          id: 'med-1',
          driverId: 'test-driver-123',
          assessmentDate: '2026-01-15',
          validFrom: '2026-01-15',
          validUntil: '2027-01-15',
          fitnessStatus: 'FIT',
          visionTestStatus: 'PASSED',
          restrictions: null,
          examinerOrProvider: 'Dr. Jane Smith',
          certificateReference: 'MED-2026-001',
          remarks: 'Fit for all commercial driving',
          active: true,
          createdAt: '2026-01-15T10:00:00Z',
          updatedAt: '2026-01-15T10:00:00Z',
        },
      ],
    });

    renderComponent();

    expect(screen.getByText('Medical Fitness & Certificates (US-43)')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('FIT')).toBeDefined();
      expect(screen.getByText('2026-01-15')).toBeDefined();
      expect(screen.getByText('2026-01-15 to 2027-01-15')).toBeDefined();
      expect(screen.getByText('PASSED')).toBeDefined();
      expect(screen.getByText('MED-2026-001')).toBeDefined();
      expect(screen.getByText('Dr. Jane Smith')).toBeDefined();
    });
  });
});
