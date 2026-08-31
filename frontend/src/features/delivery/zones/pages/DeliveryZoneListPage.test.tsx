import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DeliveryZoneListPage from './DeliveryZoneListPage';

// Mock AuthContext
vi.mock('../../../../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: () => true,
  }),
}));

// Mock deliveryZoneApi
vi.mock('../api/deliveryZoneApi', () => ({
  deliveryZoneApi: {
    list: vi.fn().mockResolvedValue([
      {
        id: '11111111-1111-1111-1111-111111111111',
        zoneCode: 'ZONE-COL-01',
        zoneName: 'Colombo Central',
        zoneType: 'URBAN_DENSE',
        status: 'ACTIVE',
        serviceable: true,
        priority: 10,
        dailyCapacity: 100,
        coordinates: [
          { longitude: 79.8, latitude: 6.9 },
          { longitude: 79.9, latitude: 6.9 },
          { longitude: 79.9, latitude: 7.0 },
          { longitude: 79.8, latitude: 7.0 },
          { longitude: 79.8, latitude: 6.9 },
        ],
        minLatitude: 6.9,
        maxLatitude: 7.0,
        minLongitude: 79.8,
        maxLongitude: 79.9,
        approximateArea: 0.01,
        version: 0,
        createdAt: '2026-08-31T10:00:00Z',
        createdBy: 'admin',
        updatedAt: '2026-08-31T10:00:00Z',
        updatedBy: 'admin',
      },
    ]),
  },
}));

describe('DeliveryZoneListPage', () => {
  it('renders delivery zone header and table', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <DeliveryZoneListPage />
      </QueryClientProvider>
    );

    expect(screen.getByText('Delivery Zones')).toBeDefined();
    expect(screen.getByText('Create Delivery Zone')).toBeDefined();
    expect(await screen.findByText('ZONE-COL-01')).toBeDefined();
    expect(screen.getByText('Colombo Central')).toBeDefined();
  });
});
