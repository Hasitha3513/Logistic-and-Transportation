import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import DriverExceptionSection from './DriverExceptionSection';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: (perm: string) => perm === 'DRIVER_EXCEPTION_MANAGE' || perm === 'DRIVER_VIEW',
    user: { username: 'fleet.manager' },
  }),
}));

const mockGet = vi.fn();
const mockPost = vi.fn();
const mockPatch = vi.fn();

vi.mock('../api/client', () => ({
  api: {
    get: (url: string) => mockGet(url),
    post: (url: string, data: any) => mockPost(url, data),
    patch: (url: string, data: any) => mockPatch(url, data),
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
        <DriverExceptionSection driverId={driverId} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('DriverExceptionSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders driver exceptions section header and exceptions table', async () => {
    mockGet.mockResolvedValueOnce({
      data: [
        {
          id: 'e-1',
          driverId: 'test-driver-123',
          exceptionType: 'LEAVE',
          startTime: '2026-09-01T08:00:00Z',
          endTime: '2026-09-01T16:00:00Z',
          status: 'SCHEDULED',
          reason: 'Annual Leave',
          remarks: 'Approved by Dispatcher',
          createdAt: '2026-08-19T10:00:00Z',
          updatedAt: '2026-08-19T10:00:00Z',
        },
      ],
    });

    renderComponent();

    expect(screen.getByText('Driver Exceptions & Leave')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('Leave')).toBeDefined();
      expect(screen.getByText('Scheduled')).toBeDefined();
      expect(screen.getByText('Annual Leave')).toBeDefined();
      expect(screen.getByText('Approved by Dispatcher')).toBeDefined();
      expect(screen.getByText('Edit')).toBeDefined();
      expect(screen.getByText('Done')).toBeDefined();
      expect(screen.getByText('Cancel')).toBeDefined();
    });
  });

  it('renders empty table when no exceptions exist', async () => {
    mockGet.mockResolvedValueOnce({ data: [] });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('No exceptions or leaves recorded for this driver')).toBeDefined();
    });
  });
});
