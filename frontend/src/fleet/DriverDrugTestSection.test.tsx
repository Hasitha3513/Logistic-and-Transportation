import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DriverDrugTestSection } from './DriverDrugTestSection';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: (perm: string) => perm === 'DRIVER_DRUG_TEST_MANAGE' || perm === 'DRIVER_DRUG_TEST_VIEW',
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
        <DriverDrugTestSection driverId={driverId} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('DriverDrugTestSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders driver drug tests section and table', async () => {
    mockGet.mockResolvedValueOnce({
      data: [
        {
          id: 'dt-1',
          driverId: 'test-driver-123',
          testType: 'RANDOM',
          scheduledDate: '2026-06-01',
          sampleCollectedAt: '2026-06-01T09:00:00Z',
          resultDate: '2026-06-02',
          result: 'NEGATIVE',
          status: 'COMPLETED',
          laboratoryOrProvider: 'LabCorp',
          referenceNumber: 'REF-DT-100',
          remarks: 'All panels negative',
          returnToDutyRequired: false,
          returnToDutyClearedAt: null,
          active: true,
          createdAt: '2026-06-01T10:00:00Z',
          updatedAt: '2026-06-02T10:00:00Z',
        },
      ],
    });

    renderComponent();

    expect(screen.getByText('Substance Screening & Drug Tests (US-44)')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('RANDOM')).toBeDefined();
      expect(screen.getByText('2026-06-01')).toBeDefined();
      expect(screen.getByText('COMPLETED')).toBeDefined();
      expect(screen.getByText('NEGATIVE')).toBeDefined();
      expect(screen.getByText('LabCorp')).toBeDefined();
      expect(screen.getByText('REF-DT-100')).toBeDefined();
    });
  });
});
