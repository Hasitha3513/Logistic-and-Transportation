import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import FuelPerformancePage from './FuelPerformancePage';

const { metrics } = vi.hoisted(() => ({ metrics: {
  consumedLitres: 36, distanceKm: 300, engineHours: null, litresPer100Km: 12,
  kmPerLitre: 8.333, litresPerEngineHour: null, totalCost: 3600, consumptionRate: 0.12,
  adverseVariancePercent: 20, sampleCount: 3, excludedQuantity: 0, quality: 'COMPLETE' as const,
  exclusionReasons: {}, indicators: ['EFFICIENCY_DEVIATION' as const], currency: 'LKR',
  baseline: { type: 'SAME_VEHICLE_PRECEDING_EQUAL_WINDOW', period: null, sampleCount: 3, rate: 0.1 },
} }));

vi.mock('../api/fuelPerformanceApi', () => ({
  fuelPerformanceApi: {
    summary: vi.fn().mockResolvedValue({
      period: { from: '2026-08-06', to: '2026-09-04', timeZone: 'Asia/Colombo' },
      measurementMode: 'DISTANCE', metrics, vehicleCount: 1, driverCount: 1,
      calculatedAt: '2026-09-04T12:00:00Z',
    }),
    vehicles: vi.fn().mockResolvedValue({ content: [{
      vehicleId: 'v1', vehicleLabel: 'WP-TEST', vehicleTypeId: 't1', fuelType: 'DIESEL',
      measurementMode: 'DISTANCE', metrics, peerRate: null, calculatedAt: '2026-09-04T12:00:00Z',
    }], page: 0, size: 20, totalElements: 1, totalPages: 1 }),
    drivers: vi.fn().mockResolvedValue({ content: [{
      driverId: 'd1', driverLabel: 'Test Driver', fuelType: 'DIESEL', measurementMode: 'DISTANCE',
      metrics, calculatedAt: '2026-09-04T12:00:00Z',
    }], page: 0, size: 20, totalElements: 1, totalPages: 1 }),
    trends: vi.fn().mockResolvedValue([{ bucketStart: '2026-09-04', bucketEnd: '2026-09-04',
      grain: 'DAILY', actualRate: 0.12, baselineRate: 0.1, percentChange: 20,
      quality: 'COMPLETE', indicators: ['EFFICIENCY_DEVIATION'] }]),
  },
}));

describe('FuelPerformancePage', () => {
  it('shows non-punitive vehicle and driver analytics with lineage', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={client}><FuelPerformancePage /></QueryClientProvider>);
    expect(await screen.findByText('WP-TEST')).toBeInTheDocument();
    expect(screen.getByText('Test Driver')).toBeInTheDocument();
    expect(screen.getAllByText('EFFICIENCY DEVIATION').length).toBeGreaterThan(0);
    expect(screen.getByText(/Asia\/Colombo/)).toBeInTheDocument();
    expect(screen.queryByText(/worst driver/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/theft|fraud|guilty/i)).not.toBeInTheDocument();
  });
});
