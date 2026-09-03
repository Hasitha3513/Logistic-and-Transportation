import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DeliveryBatchListPage } from '../pages/DeliveryBatchListPage';

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
vi.mock('../../slots/api/deliverySlotApi', () => ({
  deliverySlotApi: {
    list: vi.fn().mockResolvedValue([
      {
        id: '22222222-2222-2222-2222-222222222222',
        slotName: 'Morning Slot',
      },
    ]),
  },
}));

// Mock deliveryRiderApi
vi.mock('../../riders/api/deliveryRiderApi', () => ({
  deliveryRiderApi: {
    getRiders: vi.fn().mockResolvedValue({
      data: [
        {
          id: '33333333-3333-3333-3333-333333333333',
          riderCode: 'RDR-001',
          riderType: 'FULL_TIME',
        },
      ],
    }),
  },
}));

// Mock deliveryBatchApi
vi.mock('../api/deliveryBatchApi', () => ({
  deliveryBatchApi: {
    getBatches: vi.fn().mockResolvedValue({
      content: [
        {
          id: '44444444-4444-4444-4444-444444444444',
          tenantId: '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a',
          batchCode: 'BAT-2026-000001',
          deliveryZoneId: '11111111-1111-1111-1111-111111111111',
          deliverySlotId: '22222222-2222-2222-2222-222222222222',
          status: 'DRAFT',
          maxBatchSize: 5,
          activeOrderCount: 2,
          totalOrderCount: 2,
          version: 0,
          createdAt: '2026-09-01T10:00:00Z',
          updatedAt: '2026-09-01T10:00:00Z',
          createdBy: 'admin',
          updatedBy: 'admin',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    }),
    getBatchOrders: vi.fn().mockResolvedValue([]),
    createBatch: vi.fn(),
    autoCluster: vi.fn(),
    assignRider: vi.fn(),
    dispatchBatch: vi.fn(),
    cancelBatch: vi.fn(),
  },
}));

describe('DeliveryBatchListPage', () => {
  it('renders batch list header and batches table', async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <DeliveryBatchListPage />
      </QueryClientProvider>
    );

    expect(screen.getByText('Delivery Batches & Clustering')).toBeDefined();
    expect(await screen.findByText('BAT-2026-000001')).toBeDefined();
    expect(screen.getByText('Colombo Central')).toBeDefined();
    expect(screen.getByText('DRAFT')).toBeDefined();
    expect(screen.getByText('Auto-Cluster')).toBeDefined();
    expect(screen.getByText('Create Batch')).toBeDefined();
  });
});
