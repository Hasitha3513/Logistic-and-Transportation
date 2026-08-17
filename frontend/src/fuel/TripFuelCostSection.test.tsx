import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import TripFuelCostSection from './TripFuelCostSection';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: () => true,
    user: { username: 'fuel.manager' },
  }),
}));

vi.mock('../api/client', () => ({
  api: {
    get: vi.fn((url: string) => {
      if (url.includes('/trips/trip-complete/fuel-cost')) {
        return Promise.resolve({
          data: {
            tripId: 'trip-complete',
            vehicleId: 'veh-1',
            totalFuelQuantityLiters: 30,
            currencyCode: 'LKR',
            totalFuelCost: 9100,
            tripDistanceKm: 200,
            costPerKm: 45.5,
            litersPer100Km: 15.0,
            fuelIssueCount: 1,
            unpricedIssueCount: 0,
            distanceStatus: 'CALCULATED',
            calculationStatus: 'COMPLETE',
            lines: [
              {
                fuelIssueId: 'issue-1',
                voucherNumber: 'V-001',
                issuedAt: '2026-08-18T10:00:00Z',
                quantityLiters: 30,
                unitPrice: 303.33,
                lineCost: 9100,
                pricingSource: 'EXPLICIT_ISSUE_PRICE',
                currencyCode: 'LKR',
                stationId: 'st-1',
                fuelType: 'DIESEL',
              },
            ],
            calculatedAt: '2026-08-18T12:00:00Z',
          },
        });
      }
      if (url.includes('/trips/trip-partial/fuel-cost')) {
        return Promise.resolve({
          data: {
            tripId: 'trip-partial',
            vehicleId: 'veh-1',
            totalFuelQuantityLiters: 20,
            currencyCode: 'LKR',
            totalFuelCost: 6000,
            tripDistanceKm: 200,
            costPerKm: null,
            litersPer100Km: null,
            fuelIssueCount: 2,
            unpricedIssueCount: 1,
            distanceStatus: 'CALCULATED',
            calculationStatus: 'PARTIAL',
            lines: [],
            calculatedAt: '2026-08-18T12:00:00Z',
          },
        });
      }
      return Promise.resolve({ data: null });
    }),
  },
}));

describe('TripFuelCostSection', () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  it('renders fuel cost statistics and table lines for a complete calculation', async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <TripFuelCostSection tripId="trip-complete" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('Total Fuel Cost')).toBeInTheDocument();
    expect(screen.getByText('9,100')).toBeInTheDocument();
    expect(screen.getByText('Total Fuel Quantity')).toBeInTheDocument();
    expect(screen.getByText('30.000')).toBeInTheDocument();
    expect(screen.getByText('V-001')).toBeInTheDocument();
    expect(screen.getByText('Issue Price')).toBeInTheDocument();
  });

  it('displays warning alert when calculation status is partial due to unpriced issues', async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <TripFuelCostSection tripId="trip-partial" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('Incomplete Pricing Data')).toBeInTheDocument();
  });
});