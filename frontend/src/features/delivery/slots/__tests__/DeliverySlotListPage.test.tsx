import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DeliverySlotListPage from '../pages/DeliverySlotListPage';

// Mock matchMedia for Ant Design components in jsdom
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock AuthContext
vi.mock('../../../../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: () => true,
  }),
}));

// Mock deliveryZoneApi
vi.mock('../../zones/api/deliveryZoneApi', () => ({
  deliveryZoneApi: {
    list: vi.fn().mockResolvedValue([
      {
        id: '11111111-1111-1111-1111-111111111111',
        zoneCode: 'ZONE-COL-01',
        zoneName: 'Colombo Central',
      },
    ]),
  },
}));

// Mock deliverySlotApi
vi.mock('../api/deliverySlotApi', () => ({
  deliverySlotApi: {
    list: vi.fn().mockResolvedValue([
      {
        id: '22222222-2222-2222-2222-222222222222',
        tenantId: '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a',
        deliveryZoneId: '11111111-1111-1111-1111-111111111111',
        slotDate: '2026-09-01',
        startTime: '09:00:00',
        endTime: '12:00:00',
        slotType: 'STANDARD',
        maxCapacity: 10,
        reservedCapacity: 4,
        remainingCapacity: 6,
        bufferMinutes: 15,
        status: 'ACTIVE',
        version: 1,
        createdAt: '2026-08-31T10:00:00Z',
        createdBy: 'admin',
        updatedAt: '2026-08-31T10:00:00Z',
        updatedBy: 'admin',
      },
    ]),
    listReservations: vi.fn().mockResolvedValue([]),
  },
}));

describe('DeliverySlotListPage', () => {
  it('renders delivery slots page header and table', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <DeliverySlotListPage />
      </QueryClientProvider>
    );

    expect(screen.getByText('Delivery Slots & Capacity Management')).toBeInTheDocument();
    expect(await screen.findByText('09:00 - 12:00')).toBeInTheDocument();
    expect(screen.getByText('STANDARD')).toBeInTheDocument();
    expect(screen.getByText('4 / 10 booked')).toBeInTheDocument();
  });
});
