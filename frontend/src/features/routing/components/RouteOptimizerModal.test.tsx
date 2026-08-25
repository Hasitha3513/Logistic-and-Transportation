import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { RouteOptimizerModal } from './RouteOptimizerModal';

const mockPost = vi.fn();

vi.mock('../../../api/client', () => ({
  api: {
    get: vi.fn(),
    post: (url: string, body?: unknown) => mockPost(url, body),
  },
}));

function renderComponent(props = { open: true, routeId: 'route-opt-1', onClose: vi.fn(), onApplied: vi.fn() }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <AntApp>
        <RouteOptimizerModal {...props} />
      </AntApp>
    </QueryClientProvider>
  );
}

describe('RouteOptimizerModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders optimization preview with potential savings and allows applying', async () => {
    mockPost.mockImplementation((url: string) => {
      if (url.endsWith('/optimize')) {
        return Promise.resolve({
          data: {
            routeId: 'route-opt-1',
            originalStopLocationIds: ['s1', 's2'],
            optimizedStopLocationIds: ['s2', 's1'],
            originalEstimatedDistanceKm: 100.0,
            optimizedEstimatedDistanceKm: 80.0,
            originalEstimatedDurationMinutes: 120,
            optimizedEstimatedDurationMinutes: 96,
            distanceSavedKm: 20.0,
            durationSavedMinutes: 24,
            percentageDistanceImprovement: 20.0,
          },
        });
      }
      if (url.endsWith('/apply-optimization')) {
        return Promise.resolve({
          data: {
            id: 'route-opt-1',
            code: 'RT-1',
            name: 'Optimized Route',
          },
        });
      }
      return Promise.reject(new Error('Unknown url'));
    });

    const onApplied = vi.fn();
    const onClose = vi.fn();

    renderComponent({ open: true, routeId: 'route-opt-1', onClose, onApplied });

    await waitFor(() => {
      expect(screen.getByText(/Route Stop Optimizer/i)).toBeInTheDocument();
      expect(screen.getByText(/Optimization found!/i)).toBeInTheDocument();
      expect(screen.getAllByText(/20 km/i).length).toBeGreaterThan(0);
      expect(screen.getByText('Before')).toBeInTheDocument();
      expect(screen.getByText('After')).toBeInTheDocument();
    });

    const applyBtn = screen.getByRole('button', { name: /Apply Optimization/i });
    expect(applyBtn).not.toBeDisabled();

    fireEvent.click(applyBtn);

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/routes/route-opt-1/apply-optimization', {
        optimizedStopLocationIds: ['s2', 's1'],
      });
      expect(onApplied).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('shows the backend error and never applies when preview fails', async () => {
    mockPost.mockRejectedValueOnce({ response: { data: { message: 'Coordinates are required for every route location' } } });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Optimization Failed')).toBeInTheDocument();
      expect(screen.getByText('Coordinates are required for every route location')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /Apply Optimization/i })).toBeDisabled();
  });

  it('displays already optimal alert when savings are zero', async () => {
    mockPost.mockImplementation((url: string) => {
      if (url.endsWith('/optimize')) {
        return Promise.resolve({
          data: {
            routeId: 'route-opt-zero',
            originalStopLocationIds: ['s1'],
            optimizedStopLocationIds: ['s1'],
            originalEstimatedDistanceKm: 50.0,
            optimizedEstimatedDistanceKm: 50.0,
            originalEstimatedDurationMinutes: 45,
            optimizedEstimatedDurationMinutes: 45,
            distanceSavedKm: 0.0,
            durationSavedMinutes: 0,
            percentageDistanceImprovement: 0.0,
          },
        });
      }
      return Promise.reject(new Error('Unknown url'));
    });

    renderComponent({ open: true, routeId: 'route-opt-zero', onClose: vi.fn(), onApplied: vi.fn() });

    await waitFor(() => {
      expect(screen.getByText(/The current stop sequence is already optimal/i)).toBeInTheDocument();
    });

    const applyBtn = screen.getByRole('button', { name: /Apply Optimization/i });
    expect(applyBtn).toBeDisabled();
  });
});
