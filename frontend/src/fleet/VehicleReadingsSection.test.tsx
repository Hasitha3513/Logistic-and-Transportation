import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { describe, expect, it, vi } from 'vitest';
import VehicleReadingsSection from './VehicleReadingsSection';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: () => true,
    user: { username: 'fuel.manager' },
  }),
}));

vi.mock('../api/client', () => ({
  api: {
    get: vi.fn((url: string) => {
      if (url.includes('/latest')) {
        return Promise.resolve({
          data: {
            vehicleId: 'test-vehicle-id',
            odometer: { value: 12500, unit: 'KILOMETERS', meterEpoch: 0, recordedAt: '2026-08-16T10:00:00Z' },
            engineHours: { value: 450, unit: 'HOURS', meterEpoch: 0, recordedAt: '2026-08-16T10:00:00Z' },
          },
        });
      }
      if (url.includes('/mileage')) {
        return Promise.resolve({
          data: {
            vehicleId: 'test-vehicle-id',
            distanceTravelledKm: 500,
            engineHoursUsed: 20,
            meterResetCount: 0,
            coverageStatus: 'COMPLETE',
            abnormalDetected: false,
          },
        });
      }
      if (url.includes('/meter-resets')) {
        return Promise.resolve({ data: [] });
      }
      if (url.includes('/readings')) {
        return Promise.resolve({
          data: {
            content: [
              {
                id: 'r-1',
                vehicleId: 'test-vehicle-id',
                readingType: 'ODOMETER',
                value: 12500,
                unit: 'KILOMETERS',
                meterEpoch: 0,
                sourceType: 'MANUAL',
                recordedAt: '2026-08-16T10:00:00Z',
              },
            ],
            totalElements: 1,
            totalPages: 1,
            page: 0,
            limit: 10,
          },
        });
      }
      return Promise.resolve({ data: {} });
    }),
    post: vi.fn(() => Promise.resolve({ data: {} })),
  },
}));

describe('VehicleReadingsSection', () => {
  it('renders snapshots and mileage statistics', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <AntApp>
          <VehicleReadingsSection vehicleId="test-vehicle-id" />
        </AntApp>
      </QueryClientProvider>
    );

    expect(screen.getByText('Vehicle Mileage & Readings')).toBeDefined();
    expect(screen.getByText('Current Odometer')).toBeDefined();
    expect(screen.getByText('Current Engine Hours')).toBeDefined();
    expect(screen.getByText('Record Reading')).toBeDefined();
    expect(screen.getByText('Reset Meter')).toBeDefined();
  });
});