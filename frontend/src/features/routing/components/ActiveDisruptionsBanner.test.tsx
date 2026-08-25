import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ActiveDisruptionsBanner } from './ActiveDisruptionsBanner';

const mockGet = vi.fn();

vi.mock('../../../api/client', () => ({
  api: {
    get: (url: string) => mockGet(url),
  },
}));

function renderComponent() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ActiveDisruptionsBanner />
    </QueryClientProvider>
  );
}

describe('ActiveDisruptionsBanner', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders banner when active disruptions exist', async () => {
    mockGet.mockResolvedValueOnce({
      data: [
        {
          id: 'dis-1',
          routeId: 'r-1',
          disruptionType: 'ROAD_CLOSURE',
          severity: 'CRITICAL',
          description: 'Expressway landslide',
          effectiveFrom: '2026-08-24T10:00:00Z',
          status: 'ACTIVE',
        },
      ],
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/1 Active Route Disruption\(s\) in Network:/)).toBeInTheDocument();
      expect(screen.getByText(/Expressway landslide/)).toBeInTheDocument();
    });
  });

  it('renders nothing when no active disruptions exist', async () => {
    mockGet.mockResolvedValueOnce({ data: [] });

    const { container } = renderComponent();

    await waitFor(() => {
      expect(container.firstChild).toBeNull();
    });
  });
});
